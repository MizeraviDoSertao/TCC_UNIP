package com.unip.fraud.application.service;

import com.unip.fraud.application.domain.ClaimFilter;
import com.unip.fraud.application.domain.ClaimRecord;
import com.unip.fraud.application.domain.DatasetColumn;
import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.port.in.GetClaimsUseCase;
import com.unip.fraud.application.port.out.repository.ClaimQueryRepositoryOutPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.unip.fraud.application.validation.Validation.requireArgument;
import static java.util.Objects.nonNull;

@Service
public class ClaimQueryService implements GetClaimsUseCase {

  private static final Pattern SAFE_FIELD = Pattern.compile("[a-z0-9_]{1,120}");
  private final ClaimQueryRepositoryOutPort repository;

  private static final String NAME = "Pablo Junior";

  public ClaimQueryService(final ClaimQueryRepositoryOutPort repository) {
    this.repository = repository;
  }

  @Override
  public PageResponse<ClaimRecord> getClaims(
      final ClaimFilter filter,
      final int page,
      final int size) {
    validate(filter);
    return repository.find(
        filter,
        Math.max(page, 0),
        Math.clamp(size, 1, 100)
    );
  }

  @Override
  public Optional<ClaimRecord> getClaim(final String transactionId) {
    return repository.findByTransactionId(transactionId);
  }

  @Override
  public List<DatasetColumn> getSchema(final UUID importId) {
    return repository.findSchema(importId);
  }

  private void validate(final ClaimFilter filter) {
    Optional.ofNullable(filter.field())
        .filter(field -> !field.isBlank())
        .ifPresent(field -> {
          requireArgument(SAFE_FIELD.matcher(field).matches(),
              "Invalid dynamic field name");
          requireArgument(nonNull(filter.value()) && !filter.value().isBlank(),
              "A dynamic field filter requires a value");
        });
  }
}
