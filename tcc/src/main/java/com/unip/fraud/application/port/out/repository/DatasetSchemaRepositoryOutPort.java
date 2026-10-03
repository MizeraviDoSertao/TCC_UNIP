package com.unip.fraud.application.port.out.repository;

import com.unip.fraud.application.domain.DetectedColumn;

import java.util.List;
import java.util.UUID;

public interface DatasetSchemaRepositoryOutPort {
  void register(final UUID importId, final String sheetName, final List<DetectedColumn> columns);
}
