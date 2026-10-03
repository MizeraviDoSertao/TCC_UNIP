package com.unip.fraud.application.domain;

import java.util.UUID;

public record PendingTransactionEvent(
    UUID eventId,
    Transaction transaction,
    int attempts
) {
}
