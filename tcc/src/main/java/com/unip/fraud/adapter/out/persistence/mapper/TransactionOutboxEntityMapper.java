package com.unip.fraud.adapter.out.persistence.mapper;

import com.unip.fraud.adapter.out.persistence.entity.TransactionOutboxEntity;
import com.unip.fraud.application.domain.PendingTransactionEvent;
import com.unip.fraud.application.domain.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Mapper(config = PersistenceMapperConfig.class)
public interface TransactionOutboxEntityMapper {

  @Mapping(target = "publishedAt", ignore = true)
  @Mapping(target = "lastError", ignore = true)
  @Mapping(target = "attempts", constant = "0")
  TransactionOutboxEntity toEntity(
      final UUID id,
      final String aggregateId,
      final String eventType,
      final Map<String, Object> payload,
      final String status,
      final LocalDateTime createdAt);

  @Mapping(target = "eventId", source = "id")
  @Mapping(target = "transaction", expression = "java(toTransaction(entity))")
  PendingTransactionEvent toDomain(final TransactionOutboxEntity entity);

  default Transaction toTransaction(final TransactionOutboxEntity entity) {
    @SuppressWarnings("unchecked")
    final Map<String, Object> features =
        (Map<String, Object>) entity.getPayload().get("features");
    final Object rawFraud = entity.getPayload().get("realFraud");
    final Boolean realFraud = rawFraud == null
        ? null
        : Boolean.valueOf(rawFraud.toString());
    return new Transaction(entity.getAggregateId(), realFraud, features);
  }
}
