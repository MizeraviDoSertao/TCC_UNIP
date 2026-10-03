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
@Table(name = "claim", schema = "silver")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SilverTransactionEntity {

  @Id
  @Column(length = 64, nullable = false)
  private String transactionId;

  private UUID importId;

  private Long rowNumber;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "normalized_data", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> features;

  @Column(name = "confirmed_fraud")
  private Boolean realFraud;
  @Column(length = 255, nullable = false)
  private String fileName;
  @Column(nullable = false)
  private LocalDateTime processedAt;

  public void confirmFraud(final Boolean confirmedFraud) {
    this.realFraud = confirmedFraud;
  }
}
