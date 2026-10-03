package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.domain.DatasetRow;

public interface DatasetRowCursor extends AutoCloseable {
  DatasetRow read() throws Exception;
  @Override
  void close() throws Exception;
}
