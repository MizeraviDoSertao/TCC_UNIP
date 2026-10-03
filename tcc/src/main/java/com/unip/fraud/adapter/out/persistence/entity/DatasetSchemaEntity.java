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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_schema", schema = "bronze")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DatasetSchemaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private UUID importId;
  @Column(length = 255, nullable = false)
  private String sheetName;

  @Column(length = 255, nullable = false)
  private String originalName;

  @Column(length = 120, nullable = false)
  private String normalizedName;

  @Column(length = 24, nullable = false)
  private String inferredType;

  @Column(length = 24, nullable = false)
  private String columnRole;
  @Column(nullable = false)
  private LocalDateTime createdAt;

  public void updateInferredType(final String inferredType) {
    this.inferredType = inferredType;
  }
}
