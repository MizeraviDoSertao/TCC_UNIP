package com.unip.fraud.config.batch;

import com.unip.fraud.application.domain.ImportStatus;
import com.unip.fraud.application.port.out.repository.ImportJobRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.RejectedRecordRepositoryOutPort;
import com.unip.fraud.application.port.out.repository.SilverRepositoryOutPort;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class ImportJobBatchListener implements JobExecutionListener {

  private final ImportJobRepositoryOutPort importJobRepository;
  private final SilverRepositoryOutPort silverRepository;
  private final RejectedRecordRepositoryOutPort rejectedRepository;

  public ImportJobBatchListener(
      final ImportJobRepositoryOutPort importJobRepository,
      final SilverRepositoryOutPort silverRepository,
      final RejectedRecordRepositoryOutPort rejectedRepository) {
    this.importJobRepository = importJobRepository;
    this.silverRepository = silverRepository;
    this.rejectedRepository = rejectedRepository;
  }

  @Override
  public void beforeJob(final JobExecution jobExecution) {
    importJobRepository.markProcessing(importId(jobExecution), LocalDateTime.now());
  }

  @Override
  public void afterJob(final JobExecution jobExecution) {
    final UUID importId = importId(jobExecution);
    final long processed = silverRepository.countByImportId(importId);
    final long rejected = rejectedRepository.countByImportId(importId);
    final boolean completed = jobExecution.getStatus() == BatchStatus.COMPLETED;
    final ImportStatus status = completed
        ? rejected > 0 ? ImportStatus.COMPLETED_WITH_WARNINGS : ImportStatus.COMPLETED
        : ImportStatus.FAILED;
    final String error = completed ? null : jobExecution.getAllFailureExceptions().stream()
        .findFirst()
        .map(Throwable::getMessage)
        .orElse(jobExecution.getExitStatus().getExitDescription());
    importJobRepository.markFinished(
        importId,
        status,
        processed,
        rejected,
        error,
        LocalDateTime.now()
    );
  }

  private UUID importId(final JobExecution jobExecution) {
    return Optional.ofNullable(
        jobExecution.getJobParameters().getString("importId")
    ).map(UUID::fromString).orElseThrow(() ->
        new IllegalStateException("Batch job is missing importId")
    );
  }
}
