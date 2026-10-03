package com.unip.fraud.application.port.out.repository;

import java.util.Map;
import java.util.UUID;

public interface BronzeRepositoryOutPort {
  void save(
      final UUID importId,
      final String fileName,
      final String sheetName,
      final long rowNumber,
      final Map<String, Object> rawPayload
  );

  long count();
}
