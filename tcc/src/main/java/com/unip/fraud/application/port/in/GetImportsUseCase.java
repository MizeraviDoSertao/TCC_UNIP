package com.unip.fraud.application.port.in;

import com.unip.fraud.application.domain.ImportJobView;
import com.unip.fraud.application.domain.PageResponse;

import java.util.UUID;

public interface GetImportsUseCase {
  ImportJobView getImport(final UUID importId);

  PageResponse<ImportJobView> getImports(int page, int size);
}
