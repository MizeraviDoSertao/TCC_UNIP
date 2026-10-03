package com.unip.fraud.application.domain;

public record FraudReviewCommand(
    String transactionId,
    ReviewDecision decision,
    String notes,
    String reviewer
) {
}
