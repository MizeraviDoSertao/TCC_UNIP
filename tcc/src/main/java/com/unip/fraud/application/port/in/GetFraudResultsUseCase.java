package com.unip.fraud.application.port.in;

import com.unip.fraud.application.domain.FraudResult;
import com.unip.fraud.application.domain.FraudResultFilter;
import com.unip.fraud.application.domain.PageResponse;

public interface GetFraudResultsUseCase {

  PageResponse<FraudResult> getResults(final FraudResultFilter filter, final int page, final int size);
}
