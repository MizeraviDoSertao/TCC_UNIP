package com.unip.fraud.application.domain;

public record DatasetColumn(
    String name,
    String originalName,
    String type,
    String role
) {
}
