package com.unip.fraud.application.port.in;

import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.domain.RejectedRecordView;

import java.util.UUID;

public interface GetImportErrorsUseCase {
  PageResponse<RejectedRecordView> getErrors(final UUID importId, final int page, final int size);
}
