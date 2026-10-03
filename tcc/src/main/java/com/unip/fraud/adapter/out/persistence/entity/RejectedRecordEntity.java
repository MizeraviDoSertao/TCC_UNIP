package com.unip.fraud.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "rejected_record", schema = "ops")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RejectedRecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private UUID importId;

  private Long rowNumber;

  @Column(length = 255, nullable = false)
  private String fileName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "original_data", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> rawPayload;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String errorReason;
  @Column(nullable = false)
  private LocalDateTime rejectedAt;
}
