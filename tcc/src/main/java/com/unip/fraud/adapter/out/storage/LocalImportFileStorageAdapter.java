package com.unip.fraud.adapter.out.storage;

import com.unip.fraud.application.domain.ImportFileCommand;
import com.unip.fraud.application.domain.StoredImportFile;
import com.unip.fraud.application.port.out.importing.ImportFileStorageOutPort;
import com.unip.fraud.application.validation.Validation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Component
public class LocalImportFileStorageAdapter implements ImportFileStorageOutPort {

  private final Path storagePath;

  public LocalImportFileStorageAdapter(@Value("${imports.storage-path}") final String storagePath) {
    this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
  }

  @Override
  public StoredImportFile store(final UUID importId, final ImportFileCommand command) {
    try {
      final String safeName = safeFileName(command.fileName());
      Files.createDirectories(storagePath);
      final Path destination = storagePath.resolve(importId + "-" + safeName).normalize();
      Validation.requireArgument(destination.startsWith(storagePath), "Invalid file name");

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (
          final InputStream source = command.content().open();
          final DigestInputStream input = new DigestInputStream(source, digest)
      ) {
        Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
      }
      return new StoredImportFile(
          safeName,
          destination.toString(),
          HexFormat.of().formatHex(digest.digest())
      );
    } catch (IllegalArgumentException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new IllegalStateException("Could not store the uploaded dataset", exception);
    }
  }

  @Override
  public void discard(final StoredImportFile storedFile) {
    try {
      final Path target = Path.of(storedFile.path()).toAbsolutePath().normalize();
      Optional.of(target)
          .filter(value -> value.startsWith(storagePath))
          .ifPresent(this::deleteStoredFile);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not discard duplicated dataset", exception);
    }
  }

  private String safeFileName(final String originalName) {
    final Path fileName = Path.of(originalName).getFileName();
    Validation.requireArgument(
        fileName != null && !fileName.toString().isBlank(),
        "Invalid file name"
    );
    return fileName.toString().replaceAll("[^a-zA-Z0-9._ -]", "_");
  }

  private void deleteStoredFile(final Path target) {
    try {
      Files.deleteIfExists(target);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not discard duplicated dataset", exception);
    }
  }
}
