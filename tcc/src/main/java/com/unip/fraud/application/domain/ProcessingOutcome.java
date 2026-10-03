package com.unip.fraud.application.domain;

import java.util.List;

public record ProcessingOutcome(
      DatasetRow source,
    Transaction transaction,
    List<DetectedColumn> columns,
    String error
) {
  public boolean rejected() {
    return error != null;
  }

  public static ProcessingOutcome accepted(
      final DatasetRow source,
      final Transaction transaction,
      final List<DetectedColumn> columns) {
    return new ProcessingOutcome(source, transaction, List.copyOf(columns), null);
  }

  public static ProcessingOutcome rejected(
      final DatasetRow source,
      final Exception exception) {
    final String message = exception.getMessage() == null
        ? exception.getClass().getSimpleName()
        : exception.getMessage();
    return new ProcessingOutcome(source, null, List.of(), message);
  }
}
