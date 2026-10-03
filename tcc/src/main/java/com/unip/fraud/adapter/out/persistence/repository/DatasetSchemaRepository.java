package com.unip.fraud.adapter.out.persistence.repository;

import com.unip.fraud.adapter.out.persistence.entity.DatasetSchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;

public interface DatasetSchemaRepository extends JpaRepository<DatasetSchemaEntity, Long> {
  Optional<DatasetSchemaEntity> findByImportIdAndSheetNameAndOriginalName(
      UUID importId,
      String sheetName,
      String originalName
  );
}
