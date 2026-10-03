package com.unip.fraud.application.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record ProcessedTransaction(
    String transactionId,
    Boolean realFraud,
    String sourceFile,
    LocalDateTime processedAt,
    Map<String, Object> features
) {
}
