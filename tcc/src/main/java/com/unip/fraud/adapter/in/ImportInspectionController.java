package com.unip.fraud.adapter.in;

import com.unip.fraud.application.domain.DatasetColumn;
import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.domain.RejectedRecordView;
import com.unip.fraud.application.port.in.GetClaimsUseCase;
import com.unip.fraud.application.port.in.GetImportErrorsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/imports/{importId}")
public class ImportInspectionController {

  private final GetImportErrorsUseCase getImportErrorsUseCase;
  private final GetClaimsUseCase getClaimsUseCase;

  public ImportInspectionController(
      final GetImportErrorsUseCase getImportErrorsUseCase,
      final GetClaimsUseCase getClaimsUseCase) {
    this.getImportErrorsUseCase = getImportErrorsUseCase;
    this.getClaimsUseCase = getClaimsUseCase;
  }

  @GetMapping("/errors")
  public PageResponse<RejectedRecordView> errors(
      @PathVariable final UUID importId,
      @RequestParam(defaultValue = "0") final int page,
      @RequestParam(defaultValue = "20") final int size) {
    return getImportErrorsUseCase.getErrors(importId, page, size);
  }

  @GetMapping("/schema")
  public List<DatasetColumn> schema(@PathVariable final UUID importId) {
    return getClaimsUseCase.getSchema(importId);
  }
}
