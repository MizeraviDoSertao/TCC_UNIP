package com.unip.fraud.adapter.in.batch;

import java.nio.file.Path;
import java.util.UUID;

public interface DatasetReaderStrategy {
  boolean supports(String fileName);
  DatasetRowCursor open(Path file, UUID importId, String originalFileName) throws Exception;
}
