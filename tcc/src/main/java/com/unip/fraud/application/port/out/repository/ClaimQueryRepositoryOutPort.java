package com.unip.fraud.application.port.out.repository;

import com.unip.fraud.application.domain.ClaimFilter;
import com.unip.fraud.application.domain.ClaimRecord;
import com.unip.fraud.application.domain.DatasetColumn;
import com.unip.fraud.application.domain.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimQueryRepositoryOutPort {
  PageResponse<ClaimRecord> find(final ClaimFilter filter, final int page, final int size);

  Optional<ClaimRecord> findByTransactionId(final String transactionId);

  List<DatasetColumn> findSchema(final UUID importId);
}
