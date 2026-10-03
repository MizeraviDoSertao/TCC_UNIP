package com.unip.fraud.adapter.out.persistence.adapter;

import com.unip.fraud.adapter.out.persistence.entity.DatasetSchemaEntity;
import com.unip.fraud.adapter.out.persistence.mapper.DatasetSchemaEntityMapper;
import com.unip.fraud.adapter.out.persistence.repository.DatasetSchemaRepository;
import com.unip.fraud.application.domain.DetectedColumn;
import com.unip.fraud.application.port.out.repository.DatasetSchemaRepositoryOutPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DatasetSchemaPersistenceAdapter implements DatasetSchemaRepositoryOutPort {

  private final DatasetSchemaRepository repository;
  private final DatasetSchemaEntityMapper mapper;

  public DatasetSchemaPersistenceAdapter(
      final DatasetSchemaRepository repository,
      final DatasetSchemaEntityMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public void register(final UUID importId, final String sheetName, final List<DetectedColumn> columns) {
    columns.forEach(column -> repository.findByImportIdAndSheetNameAndOriginalName(
        importId,
        sheetName,
        column.originalName()
    ).ifPresentOrElse(
        entity -> refreshInferredType(entity, column),
        () -> repository.save(mapper.toEntity(
            importId,
            sheetName,
            column,
            LocalDateTime.now()
        ))
    ));
  }

  private void refreshInferredType(
      final DatasetSchemaEntity entity,
      final DetectedColumn column) {
    java.util.Optional.of(entity)
        .filter(value -> "NULL".equals(value.getInferredType()))
        .filter(value -> !"NULL".equals(column.type()))
        .map(value -> {
          value.updateInferredType(column.type());
          return value;
        })
        .ifPresent(repository::save);
  }
}
