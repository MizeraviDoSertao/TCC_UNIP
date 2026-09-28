package com.unip.fraud.adapter.out.persistence.adapter;

import com.unip.fraud.adapter.out.persistence.entity.GoldFraudResultEntity;
import com.unip.fraud.adapter.out.persistence.repository.GoldFraudResultRepository;
import com.unip.fraud.application.domain.FraudResult;
import com.unip.fraud.application.domain.FraudResultFilter;
import com.unip.fraud.application.port.out.repository.GoldRepositoryOutPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class GoldPersistenceAdapter implements GoldRepositoryOutPort {

  private final GoldFraudResultRepository goldFraudResultRepository;

  public GoldPersistenceAdapter(
      final GoldFraudResultRepository goldFraudResultRepository) {
    this.goldFraudResultRepository = goldFraudResultRepository;
  }

  @Override
  public void save(final FraudResult result) {
    final GoldFraudResultEntity entity = new GoldFraudResultEntity();
    entity.setTransactionId(result.transactionId());
    entity.setRealFraud(result.realFraud());
    entity.setPredictedFraud(result.predictedFraud());
    entity.setProbability(result.probability());
    entity.setClassification(result.classification());
    entity.setProcessedAt(result.processedAt());
    goldFraudResultRepository.save(entity);

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
  public Page<FraudResult> findByFilter(FraudResultFilter filter, int page, int size) {
    final Specification<GoldFraudResultEntity> specification =
        buildSpecification(filter);

    final Pageable pageable = PageRequest.of(
        page,
        size,
        Sort.by(Sort.Direction.DESC, "processedAt"));

    return goldFraudResultRepository.findAll(specification, pageable).map(this::toDomain);
  }

  private Specification<GoldFraudResultEntity> buildSpecification(
      FraudResultFilter filter
  ) {
    Specification<GoldFraudResultEntity> specification = null;

    if (filter.predictedFraud() != null) {
      specification = addSpecification(
          specification,
          (root, query, criteriaBuilder) ->
              criteriaBuilder.equal(
                  root.get("predictedFraud"),
                  filter.predictedFraud()
              )
      );
    }

    if (filter.realFraud() != null) {
      specification = addSpecification(
          specification,
          (root, query, criteriaBuilder) ->
              criteriaBuilder.equal(
                  root.get("realFraud"),
                  filter.realFraud()
              )
      );
    }

    if (filter.minProbability() != null) {
      specification = addSpecification(
          specification,
          (root, query, criteriaBuilder) ->
              criteriaBuilder.greaterThanOrEqualTo(
                  root.get("probability"),
                  filter.minProbability()
              )
      );
    }

    if (filter.startDate() != null) {
      specification = addSpecification(
          specification,
          (root, query, criteriaBuilder) ->
              criteriaBuilder.greaterThanOrEqualTo(
                  root.get("processedAt"),
                  filter.startDate()
              )
      );
    }

    if (filter.endDate() != null) {
      specification = addSpecification(
          specification,
          (root, query, criteriaBuilder) ->
              criteriaBuilder.lessThanOrEqualTo(
                  root.get("processedAt"),
                  filter.endDate()
              )
      );
    }

    return specification;
  }

  private Specification<GoldFraudResultEntity> addSpecification(
      Specification<GoldFraudResultEntity> specification,
      Specification<GoldFraudResultEntity> newSpec
  ) {
    if (specification == null) {
      return newSpec;
    }
    return specification.and(newSpec);
  }

  private FraudResult toDomain(GoldFraudResultEntity entity) {
    return new FraudResult(
        entity.getTransactionId(),
        entity.getRealFraud(),
        entity.getPredictedFraud(),
        entity.getProbability(),
        entity.getClassification(),
        entity.getProcessedAt()
    );
  }
}
