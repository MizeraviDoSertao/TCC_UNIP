package com.unip.fraud.application.port.out.importing;

import com.unip.fraud.application.domain.ImportFileCommand;
import com.unip.fraud.application.domain.StoredImportFile;

import java.util.UUID;

public interface ImportFileStorageOutPort {
  StoredImportFile store(final UUID importId, final ImportFileCommand command);

  void discard(final StoredImportFile storedFile);
}
