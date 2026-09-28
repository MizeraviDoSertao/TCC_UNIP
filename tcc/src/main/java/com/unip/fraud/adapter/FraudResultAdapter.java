package com.unip.fraud.adapter;

import com.unip.fraud.application.domain.FraudResult;
import com.unip.fraud.application.domain.FraudResultFilter;
import com.unip.fraud.application.port.in.GetFraudResultsUseCase;
import com.unip.fraud.application.port.out.repository.GoldRepositoryOutPort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public class FraudResultAdapter implements GetFraudResultsUseCase {

  private final GoldRepositoryOutPort goldRepositoryOutPort;

  public FraudResultAdapter(
      final GoldRepositoryOutPort goldRepositoryOutPort) {
    this.goldRepositoryOutPort = goldRepositoryOutPort;
  }

  @Override
  public Page<FraudResult> getResults(FraudResultFilter filter, int page, int size) {
    return goldRepositoryOutPort.findByFilter(filter, page, size);
  }
}
