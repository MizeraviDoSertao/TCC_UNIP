package com.unip.fraud.application.port.in;

import com.unip.fraud.application.domain.FraudResult;
import com.unip.fraud.application.domain.FraudResultFilter;
import org.springframework.data.domain.Page;

public interface GetFraudResultsUseCase {

  Page<FraudResult> getResults(FraudResultFilter filter, int page, int size);
}
