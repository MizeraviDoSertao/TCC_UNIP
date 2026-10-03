package com.unip.fraud.adapter.out.persistence.mapper;

import com.unip.fraud.adapter.out.persistence.entity.RejectedRecordEntity;
import com.unip.fraud.application.domain.RejectedRecordView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Mapper(config = PersistenceMapperConfig.class)
public interface RejectedRecordEntityMapper {

  @Mapping(target = "id", ignore = true)
  RejectedRecordEntity toEntity(
      final UUID importId,
      final String fileName,
      final long rowNumber,
      final Map<String, Object> rawPayload,
      final String errorReason,
      final LocalDateTime rejectedAt);

  @Mapping(target = "originalData", source = "rawPayload")
  @Mapping(target = "rowNumber", defaultValue = "0L")
  RejectedRecordView toView(final RejectedRecordEntity entity);
}
