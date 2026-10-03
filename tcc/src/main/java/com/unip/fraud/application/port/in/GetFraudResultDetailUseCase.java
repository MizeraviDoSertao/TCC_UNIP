package com.unip.fraud.application.port.in;

import com.unip.fraud.application.domain.FraudResultDetail;

import java.util.Optional;

public interface GetFraudResultDetailUseCase {

  Optional<FraudResultDetail> getResultDetail(final String transactionId);
}
