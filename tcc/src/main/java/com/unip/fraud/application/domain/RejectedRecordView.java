package com.unip.fraud.application.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record RejectedRecordView(
    long id,
    UUID importId,
    String fileName,
    long rowNumber,
    Map<String, Object> originalData,
    String errorReason,
    LocalDateTime rejectedAt
) {
}
