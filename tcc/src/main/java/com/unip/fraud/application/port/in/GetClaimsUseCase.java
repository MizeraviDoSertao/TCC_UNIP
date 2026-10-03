package com.unip.fraud.application.port.in;

import com.unip.fraud.application.domain.ClaimFilter;
import com.unip.fraud.application.domain.ClaimRecord;
import com.unip.fraud.application.domain.DatasetColumn;
import com.unip.fraud.application.domain.PageResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetClaimsUseCase {
  PageResponse<ClaimRecord> getClaims(final ClaimFilter filter, final int page, final int size);

  Optional<ClaimRecord> getClaim(final String transactionId);

  List<DatasetColumn> getSchema(final UUID importId);
}
