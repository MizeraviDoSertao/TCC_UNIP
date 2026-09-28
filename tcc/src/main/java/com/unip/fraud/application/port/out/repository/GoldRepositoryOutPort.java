package com.unip.fraud.application.port.out.repository;

import com.unip.fraud.application.domain.FraudResult;
import com.unip.fraud.application.domain.FraudResultFilter;
import org.springframework.data.domain.Page;

public interface GoldRepositoryOutPort {
  void save(final FraudResult result);
  long count();
  long countRealFraud();
  long countPredictedFraud();
  Page<FraudResult> findByFilter(FraudResultFilter filter, int page, int size);

}
