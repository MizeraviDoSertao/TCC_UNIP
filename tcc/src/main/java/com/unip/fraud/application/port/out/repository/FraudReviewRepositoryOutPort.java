package com.unip.fraud.application.port.out.repository;

import com.unip.fraud.application.domain.FraudReview;
import com.unip.fraud.application.domain.FraudReviewCommand;

public interface FraudReviewRepositoryOutPort {
  FraudReview save(final FraudReviewCommand command);
}
