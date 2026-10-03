package com.unip.fraud.application.service;

import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.domain.RejectedRecordView;
import com.unip.fraud.application.port.in.GetImportErrorsUseCase;
import com.unip.fraud.application.port.out.repository.RejectedRecordRepositoryOutPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ImportErrorService implements GetImportErrorsUseCase {

  private final RejectedRecordRepositoryOutPort repository;

  public ImportErrorService(final RejectedRecordRepositoryOutPort repository) {
    this.repository = repository;
  }

  @Override
  public PageResponse<RejectedRecordView> getErrors(final UUID importId, final int page, final int size) {
    return repository.findByImportId(importId, Math.max(page, 0), Math.clamp(size, 1, 100));
  }
}
