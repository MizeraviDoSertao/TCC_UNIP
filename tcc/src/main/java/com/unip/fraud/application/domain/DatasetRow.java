package com.unip.fraud.application.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record DatasetRow(
    UUID importId,
    String fileName,
    String sheetName,
    long rowNumber,
    Map<String, Object> values
) {
  public DatasetRow {
    values = new LinkedHashMap<>(values);
  }
}
