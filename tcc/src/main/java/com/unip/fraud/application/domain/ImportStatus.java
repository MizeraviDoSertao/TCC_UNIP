package com.unip.fraud.application.domain;

public enum ImportStatus {
  QUEUED,
  PROCESSING,
  COMPLETED,
  COMPLETED_WITH_WARNINGS,
  FAILED
}
