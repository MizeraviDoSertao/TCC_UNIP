package com.unip.fraud.adapter.out.persistence.mapper;

import com.unip.fraud.adapter.out.persistence.entity.ImportJobEntity;
import com.unip.fraud.application.domain.ImportJob;
import org.mapstruct.Mapper;

@Mapper(config = PersistenceMapperConfig.class)
public interface ImportJobEntityMapper {

  ImportJobEntity toEntity(final ImportJob domain);

  ImportJob toDomain(final ImportJobEntity entity);
}
