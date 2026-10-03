package com.unip.fraud.adapter.out.persistence.adapter;

import com.unip.fraud.adapter.out.persistence.mapper.GoldEntityMapper;
import com.unip.fraud.adapter.out.persistence.repository.GoldFraudResultRepository;
import com.unip.fraud.adapter.out.persistence.specification.FraudResultSpecificationFactory;
import com.unip.fraud.application.domain.FraudResult;
import com.unip.fraud.application.domain.FraudResultFilter;
import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.port.out.repository.GoldRepositoryOutPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GoldPersistenceAdapter implements GoldRepositoryOutPort {

  private final GoldFraudResultRepository goldFraudResultRepository;
  private final GoldEntityMapper mapper;
  private final FraudResultSpecificationFactory specificationFactory;

  public GoldPersistenceAdapter(
      final GoldFraudResultRepository goldFraudResultRepository,
      final GoldEntityMapper mapper,
      final FraudResultSpecificationFactory specificationFactory) {
    this.goldFraudResultRepository = goldFraudResultRepository;
    this.mapper = mapper;
    this.specificationFactory = specificationFactory;
  }

  @Override
  public void save(final FraudResult result) {
    goldFraudResultRepository.save(mapper.toEntity(result));
  }

  @Override
  public long count() {
    return goldFraudResultRepository.count();
  }

  @Override
  public long countRealFraud() {
    return goldFraudResultRepository.countByRealFraudTrue();
  }

  @Override
  public long countPredictedFraud() {
    return goldFraudResultRepository.countByPredictedFraudTrue();
  }

  @Override
  public long countByRiskLevel(final String riskLevel) {
    return goldFraudResultRepository.countByRiskLevel(riskLevel);
  }

  @Override
  public long countPendingReview() {
    return goldFraudResultRepository.countByPredictedFraudIsNull();
  }

  @Override
  public PageResponse<FraudResult> findByFilter(
      final FraudResultFilter filter,
      final int page,
      final int size) {
    final Pageable pageable = PageRequest.of(
        page,
        size,
        Sort.by(Sort.Direction.DESC, "processedAt"));

    final Page<FraudResult> result = goldFraudResultRepository.findAll(
        specificationFactory.create(filter),
        pageable
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
  public Optional<FraudResult> findByTransactionId(final String transactionId) {
    return goldFraudResultRepository.findById(transactionId).map(mapper::toDomain);
  }

  @Override
  public void updateConfirmedFraud(
      final String transactionId,
      final Boolean confirmedFraud) {
    goldFraudResultRepository.findById(transactionId).ifPresent(entity -> {
      entity.confirmFraud(confirmedFraud);
      goldFraudResultRepository.save(entity);
    });
  }
}
