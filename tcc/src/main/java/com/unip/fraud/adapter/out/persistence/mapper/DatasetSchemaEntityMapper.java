package com.unip.fraud.adapter.out.persistence.mapper;

import com.unip.fraud.adapter.out.persistence.entity.DatasetSchemaEntity;
import com.unip.fraud.application.domain.DetectedColumn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.UUID;

@Mapper(config = PersistenceMapperConfig.class)
public interface DatasetSchemaEntityMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "originalName", source = "column.originalName")
  @Mapping(target = "normalizedName", source = "column.normalizedName")
  @Mapping(target = "inferredType", source = "column.type")
  @Mapping(target = "columnRole", source = "column.role")
  DatasetSchemaEntity toEntity(
      final UUID importId,
      final String sheetName,
      final DetectedColumn column,
      final LocalDateTime createdAt);
}
