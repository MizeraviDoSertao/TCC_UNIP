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
@Table(name = "claim_raw", schema = "bronze")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BronzeTransactionRawEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private UUID importId;

  @Column(length = 255)
  private String sheetName;

  private Long rowNumber;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "original_data", columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> rawPayload;

  @Column(length = 255, nullable = false)
  private String fileName;

  @Column(length = 64, nullable = false)
  private String source;
  @Column(nullable = false)
  private LocalDateTime ingestedAt;
}
