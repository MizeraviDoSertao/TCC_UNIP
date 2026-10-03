package com.unip.fraud.adapter.out.persistence.repository;

import com.unip.fraud.adapter.out.persistence.entity.RejectedRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RejectedRecordRepository extends JpaRepository<RejectedRecordEntity, Long> {
  long countByImportId(UUID importId);
  Page<RejectedRecordEntity> findByImportIdOrderByRowNumberAsc(UUID importId, Pageable pageable);
}
