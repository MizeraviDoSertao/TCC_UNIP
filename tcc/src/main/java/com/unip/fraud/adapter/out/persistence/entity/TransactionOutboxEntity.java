package com.unip.fraud.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transaction_outbox", schema = "ops")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TransactionOutboxEntity {

  @Id
  private UUID id;

  @Column(length = 64, nullable = false)
  private String aggregateId;

  @Column(length = 80, nullable = false)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> payload;

  @Column(length = 20, nullable = false)
  private String status;
  @Column(nullable = false)
  private int attempts;
  @Column(columnDefinition = "TEXT")
  private String lastError;
  @Column(nullable = false)
  private LocalDateTime createdAt;
  private LocalDateTime publishedAt;

  public void markPublished(final LocalDateTime publishedAt) {
    this.status = "PUBLISHED";
    this.publishedAt = publishedAt;
    this.lastError = null;
  }

  public void registerFailure(final String error) {
    this.attempts++;
    this.lastError = error;
  }
}
