package com.unip.fraud.application.domain;

public record StoredImportFile(
    String fileName,
    String path,
    String sha256
) {
}
