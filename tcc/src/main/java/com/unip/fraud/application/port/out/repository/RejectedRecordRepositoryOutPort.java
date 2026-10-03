package com.unip.fraud.application.port.out.repository;

import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.domain.RejectedRecordView;

import java.util.Map;
import java.util.UUID;

public interface RejectedRecordRepositoryOutPort {
  void save(
      final UUID importId,
      final String fileName,
      final long rowNumber,
      final Map<String, Object> rawPayload,
      final String errorReason
  );

  long count();

  long countByImportId(final UUID importId);

  PageResponse<RejectedRecordView> findByImportId(final UUID importId, final int page, final int size);
}
