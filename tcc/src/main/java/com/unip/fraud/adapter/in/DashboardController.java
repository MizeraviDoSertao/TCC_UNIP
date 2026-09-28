package com.unip.fraud.adapter.in;

import com.unip.fraud.application.domain.FraudResult;
import com.unip.fraud.application.domain.FraudResultFilter;
import com.unip.fraud.application.domain.PageResponse;
import com.unip.fraud.application.port.in.GetDashboardUseCase;
import com.unip.fraud.application.port.in.GetFraudResultsUseCase;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

  private final GetDashboardUseCase getDashboardUseCase;
  private final GetFraudResultsUseCase getFraudResultsUseCase;

  public DashboardController(
      final GetDashboardUseCase getDashboardUseCase,
      final GetFraudResultsUseCase getFraudResultsUseCase) {
    this.getDashboardUseCase = getDashboardUseCase;
    this.getFraudResultsUseCase = getFraudResultsUseCase;
  }

  @GetMapping("/summary")
  public Map<String, Object> getSummary() {
    return getDashboardUseCase.getSummary();
  }

  @GetMapping("/results")
  public PageResponse<FraudResult> getResults(
      @RequestParam(required = false) Boolean predictedFraud,
      @RequestParam(required = false) Boolean realFraud,
      @RequestParam(required = false) Double minProbability,

      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    FraudResultFilter filter = new FraudResultFilter(
        predictedFraud,
        realFraud,
        minProbability,
        null,
        null
    );

    Page<FraudResult> resultPage =
        getFraudResultsUseCase.getResults(filter, page, size);

    return new PageResponse<>(
        resultPage.getContent(),
        resultPage.getNumber(),
        resultPage.getSize(),
        resultPage.getTotalElements(),
        resultPage.getTotalPages()
    );
  }
}