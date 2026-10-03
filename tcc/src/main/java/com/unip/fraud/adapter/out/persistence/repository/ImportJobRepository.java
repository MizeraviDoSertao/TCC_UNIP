package com.unip.fraud.adapter.out.persistence.repository;

import com.unip.fraud.adapter.out.persistence.entity.ImportJobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, UUID> {
  Optional<ImportJobEntity> findFirstByFileHashAndStatusInOrderByCreatedAtDesc(
      String fileHash,
      Collection<String> statuses
  );

  Page<ImportJobEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
