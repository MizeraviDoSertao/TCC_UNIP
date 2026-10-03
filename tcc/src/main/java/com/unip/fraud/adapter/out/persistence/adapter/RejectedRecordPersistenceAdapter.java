package com.unip.fraud.adapter.out.persistence.adapter;

import com.unip.fraud.adapter.out.persistence.mapper.RejectedRecordEntityMapper;
import com.unip.fraud.adapter.out.persistence.repository.RejectedRecordRepository;
import com.unip.fraud.application.port.out.repository.RejectedRecordRepositoryOutPort;
import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.domain.RejectedRecordView;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class RejectedRecordPersistenceAdapter implements RejectedRecordRepositoryOutPort {

  private final RejectedRecordRepository rejectedRecordRepository;
  private final RejectedRecordEntityMapper mapper;

  public RejectedRecordPersistenceAdapter(
      final RejectedRecordRepository rejectedRecordRepository,
      final RejectedRecordEntityMapper mapper) {
    this.rejectedRecordRepository = rejectedRecordRepository;
    this.mapper = mapper;
  }

  @Override
  public void save(
      final UUID importId,
      final String fileName,
      final long rowNumber,
      final Map<String, Object> rawPayload,
      final String errorReason) {
    rejectedRecordRepository.save(mapper.toEntity(
        importId,
        fileName,
        rowNumber,
        rawPayload,
        errorReason,
        LocalDateTime.now()
    ));
  }

  @Override
  public long count() {
    return rejectedRecordRepository.count();
  }

  @Override
  public long countByImportId(final UUID importId) {
    return rejectedRecordRepository.countByImportId(importId);
  }

  @Override
  public PageResponse<RejectedRecordView> findByImportId(
      final UUID importId,
      final int page,
      final int size) {
    final var result = rejectedRecordRepository.findByImportIdOrderByRowNumberAsc(
        importId,
        PageRequest.of(page, size)
    ).map(mapper::toView);
    return new PageResponse<>(
        result.getContent(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages()
    );
  }
}
