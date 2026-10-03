package com.unip.fraud.application.domain;

public record DetectedColumn(
    String originalName,
    String normalizedName,
    String type,
    String role
) {
}
