package com.unip.fraud.adapter.out.persistence.specification;

import com.unip.fraud.adapter.out.persistence.entity.GoldFraudResultEntity;
import com.unip.fraud.application.domain.FraudResultFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
public class FraudResultSpecificationFactory {

  public Specification<GoldFraudResultEntity> create(final FraudResultFilter filter) {
    return Stream.of(
            whenPresent(filter.predictedFraud(), value -> equals("predictedFraud", value)),
            whenPresent(filter.realFraud(), value -> equals("realFraud", value)),
            whenPresent(
                filter.minProbability(),
                value -> greaterThanOrEqualTo("probability", value)
            ),
            whenPresent(filter.startDate(), value -> greaterThanOrEqualTo("processedAt", value)),
            whenPresent(filter.endDate(), value -> lessThanOrEqualTo("processedAt", value)),
            when(
                filter.riskLevel(),
                value -> !value.isBlank(),
                value -> equals("riskLevel", value.toUpperCase())
            ),
            when(
                filter.modelVersion(),
                value -> !value.isBlank(),
                value -> equals("modelVersion", value)
            )
        )
        .flatMap(Optional::stream)
        .reduce(Specification::and)
        .orElse(null);
  }

  private <T> Optional<Specification<GoldFraudResultEntity>> whenPresent(
      final T value,
      final Function<T, Specification<GoldFraudResultEntity>> factory) {
    return Optional.ofNullable(value).map(factory);
  }

  private <T> Optional<Specification<GoldFraudResultEntity>> when(
      final T value,
      final Predicate<T> condition,
      final Function<T, Specification<GoldFraudResultEntity>> factory) {
    return Optional.ofNullable(value).filter(condition).map(factory);
  }

  private Specification<GoldFraudResultEntity> equals(
      final String attribute,
      final Object value) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(attribute), value);
  }

  private <T extends Comparable<? super T>> Specification<GoldFraudResultEntity>
      greaterThanOrEqualTo(final String attribute, final T value) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.greaterThanOrEqualTo(root.get(attribute), value);
  }

  private <T extends Comparable<? super T>> Specification<GoldFraudResultEntity>
      lessThanOrEqualTo(final String attribute, final T value) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.lessThanOrEqualTo(root.get(attribute), value);
  }
}
