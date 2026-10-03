package com.unip.fraud.adapter.out.persistence.repository;

import com.unip.fraud.adapter.out.persistence.entity.SilverTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SilverTransactionRepository extends JpaRepository<SilverTransactionEntity, String> {
  long countByImportId(UUID importId);

  @Query(
      value = """
          SELECT *
          FROM silver.claim claim
          WHERE (:importId IS NULL OR claim.import_id = CAST(:importId AS UUID))
            AND (:search IS NULL OR CAST(claim.normalized_data AS TEXT) ILIKE CONCAT('%', :search, '%'))
            AND (
              :field IS NULL
              OR (claim.normalized_data ->> :field) ILIKE CONCAT('%', :fieldValue, '%')
            )
          ORDER BY claim.processed_at DESC
          """,
      countQuery = """
          SELECT COUNT(*)
          FROM silver.claim claim
          WHERE (:importId IS NULL OR claim.import_id = CAST(:importId AS UUID))
            AND (:search IS NULL OR CAST(claim.normalized_data AS TEXT) ILIKE CONCAT('%', :search, '%'))
            AND (
              :field IS NULL
              OR (claim.normalized_data ->> :field) ILIKE CONCAT('%', :fieldValue, '%')
            )
          """,
      nativeQuery = true
  )
  Page<SilverTransactionEntity> findDynamic(
      @Param("importId") String importId,
      @Param("search") String search,
      @Param("field") String field,
      @Param("fieldValue") String fieldValue,
      Pageable pageable
  );
}
