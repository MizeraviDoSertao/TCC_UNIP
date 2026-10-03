package com.unip.fraud.application.domain;

public enum ReviewDecision {
  FRAUD,
  LEGITIMATE,
  INCONCLUSIVE;

  public Boolean confirmedFraud() {
    return switch (this) {
      case FRAUD -> true;
      case LEGITIMATE -> false;
      case INCONCLUSIVE -> null;
    };
  }
}
