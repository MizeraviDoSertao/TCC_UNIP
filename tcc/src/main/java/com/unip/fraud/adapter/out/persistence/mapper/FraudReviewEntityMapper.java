package com.unip.fraud.adapter.out.persistence.mapper;

import com.unip.fraud.adapter.out.persistence.entity.FraudReviewEntity;
import com.unip.fraud.application.domain.FraudReview;
import com.unip.fraud.application.domain.FraudReviewCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(config = PersistenceMapperConfig.class)
public interface FraudReviewEntityMapper {

  @Mapping(target = "id", ignore = true)
  FraudReviewEntity toEntity(
      final FraudReviewCommand command,
      final LocalDateTime reviewedAt);

  FraudReview toDomain(final FraudReviewEntity entity);
}
