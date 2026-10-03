package com.unip.fraud.application.port.out.repository;

import com.unip.fraud.application.domain.ImportJob;
import com.unip.fraud.application.domain.ImportStatus;
import com.unip.fraud.application.domain.PageResponse;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ImportJobRepositoryOutPort {
  ImportJob save(final ImportJob importJob);

  Optional<ImportJob> findById(final UUID importId);

  Optional<ImportJob> findLatestByHashAndStatusIn(final String hash, final Set<ImportStatus> statuses);

  PageResponse<ImportJob> findAll(final int page, final int size);

  void markProcessing(final UUID importId, final LocalDateTime startedAt);

  void markQueued(final UUID importId);

  void markBatchExecutionId(final UUID importId, final long batchExecutionId);

  void markFinished(
      final UUID importId,
      final ImportStatus status,
      final long processedRows,
      final long rejectedRows,
      final String errorMessage,
      final LocalDateTime finishedAt
  );
}
