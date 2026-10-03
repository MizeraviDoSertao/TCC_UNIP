package com.unip.fraud.adapter.out.persistence.mapper;

import com.unip.fraud.adapter.out.persistence.entity.GoldFraudResultEntity;
import com.unip.fraud.application.domain.FraudResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PersistenceMapperConfig.class)
public interface GoldEntityMapper {

  @Mapping(
      target = "reasons",
      expression = "java(result.reasons() == null ? java.util.List.of() : result.reasons())"
  )
  GoldFraudResultEntity toEntity(final FraudResult result);

  FraudResult toDomain(final GoldFraudResultEntity entity);
}
