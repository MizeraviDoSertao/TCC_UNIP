package com.unip.fraud.application.domain;

import java.time.LocalDateTime;

public record FraudReview(
    long id,
    String transactionId,
    ReviewDecision decision,
    String notes,
    String reviewer,
    LocalDateTime reviewedAt
) {
}
