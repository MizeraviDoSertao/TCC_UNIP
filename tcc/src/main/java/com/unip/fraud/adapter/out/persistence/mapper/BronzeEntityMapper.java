package com.unip.fraud.adapter.out.persistence.mapper;

import com.unip.fraud.adapter.out.persistence.entity.BronzeTransactionRawEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Mapper(config = PersistenceMapperConfig.class)
public interface BronzeEntityMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "source", constant = "uploaded-file")
  BronzeTransactionRawEntity toEntity(
      final UUID importId,
      final String fileName,
      final String sheetName,
      final long rowNumber,
      final Map<String, Object> rawPayload,
      final LocalDateTime ingestedAt);
}
