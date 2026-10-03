package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.domain.DatasetRow;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import java.nio.file.Path;
import java.util.UUID;

public class DynamicDatasetItemReader implements ItemStreamReader<DatasetRow> {

  private static final String RECORD_INDEX_KEY = "dynamicDataset.recordIndex";

  private final DatasetReaderFactory factory;
  private final Path file;
  private final UUID importId;
  private final String originalFileName;
  private DatasetRowCursor cursor;
  private long recordIndex;

  public DynamicDatasetItemReader(
      final DatasetReaderFactory factory,
      final String filePath,
      final String importId,
      final String originalFileName) {
    this.factory = factory;
    this.file = Path.of(filePath);
    this.importId = UUID.fromString(importId);
    this.originalFileName = originalFileName;
  }

  @Override
  public void open(final ExecutionContext executionContext) throws ItemStreamException {
    try {
      cursor = factory.open(file, importId, originalFileName);
      final long alreadyRead = executionContext.getLong(RECORD_INDEX_KEY, 0);
      while (recordIndex < alreadyRead && cursor.read() != null) {
        recordIndex++;
      }
    } catch (Exception exception) {
      throw new ItemStreamException("Could not open dataset " + originalFileName, exception);
    }
  }

  @Override
  public DatasetRow read() throws Exception {
    final DatasetRow row = cursor.read();
    if (row != null) {
      recordIndex++;
    }
    return row;
  }

  @Override
  public void update(final ExecutionContext executionContext) {
    executionContext.putLong(RECORD_INDEX_KEY, recordIndex);
  }

  @Override
  public void close() throws ItemStreamException {
    if (cursor == null) {
      return;
    }
    try {
      cursor.close();
    } catch (Exception exception) {
      throw new ItemStreamException("Could not close dataset " + originalFileName, exception);
    }
  }
}
