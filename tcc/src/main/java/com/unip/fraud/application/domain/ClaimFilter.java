package com.unip.fraud.application.domain;

import java.util.UUID;

public record ClaimFilter(
    UUID importId,
    String search,
    String field,
    String value
) {
}
