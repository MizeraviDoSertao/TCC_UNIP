package com.unip.fraud.adapter.out.persistence.adapter;

import com.unip.fraud.adapter.out.persistence.entity.ImportJobEntity;
import com.unip.fraud.adapter.out.persistence.mapper.ImportJobEntityMapper;
import com.unip.fraud.adapter.out.persistence.repository.ImportJobRepository;
import com.unip.fraud.application.domain.ImportJob;
import com.unip.fraud.application.domain.ImportStatus;
import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.port.out.repository.ImportJobRepositoryOutPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class ImportJobPersistenceAdapter implements ImportJobRepositoryOutPort {

  private final ImportJobRepository repository;
  private final ImportJobEntityMapper mapper;

  public ImportJobPersistenceAdapter(
      final ImportJobRepository repository,
      final ImportJobEntityMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public ImportJob save(final ImportJob importJob) {
    return mapper.toDomain(repository.save(mapper.toEntity(importJob)));
  }

  @Override
  public Optional<ImportJob> findById(final UUID importId) {
    return repository.findById(importId).map(mapper::toDomain);
  }

  @Override
  public Optional<ImportJob> findLatestByHashAndStatusIn(final String hash, final Set<ImportStatus> statuses) {
    return repository.findFirstByFileHashAndStatusInOrderByCreatedAtDesc(
        hash,
        statuses.stream().map(Enum::name).toList()
    ).map(mapper::toDomain);
  }

  @Override
  public PageResponse<ImportJob> findAll(final int page, final int size) {
    final Page<ImportJob> result = repository.findAllByOrderByCreatedAtDesc(
        PageRequest.of(page, size)
    ).map(mapper::toDomain);
    return new PageResponse<>(
        result.getContent(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages()
    );
  }

  @Override
  public void markProcessing(final UUID importId, final LocalDateTime startedAt) {
    final ImportJobEntity entity = required(importId);
    entity.markProcessing(startedAt);
    repository.save(entity);
  }

  @Override
  public void markQueued(final UUID importId) {
    final ImportJobEntity entity = required(importId);
    entity.markQueued();
    repository.save(entity);
  }

  @Override
  public void markBatchExecutionId(final UUID importId, final long batchExecutionId) {
    final ImportJobEntity entity = required(importId);
    entity.linkBatchExecution(batchExecutionId);
    repository.save(entity);
  }

  @Override
  public void markFinished(
      final UUID importId,
      final ImportStatus status,
      final long processedRows,
      final long rejectedRows,
      final String errorMessage,
      final LocalDateTime finishedAt) {
    final ImportJobEntity entity = required(importId);
    entity.markFinished(
        status.name(),
        processedRows,
        rejectedRows,
        errorMessage,
        finishedAt
    );
    repository.save(entity);
  }

  private ImportJobEntity required(final UUID importId) {
    return repository.findById(importId)
        .orElseThrow(() -> new IllegalStateException("Import not found: " + importId));
  }

}
