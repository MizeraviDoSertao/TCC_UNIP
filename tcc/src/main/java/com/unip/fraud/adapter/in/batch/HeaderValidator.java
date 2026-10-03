package com.unip.fraud.adapter.in.batch;

import com.unip.fraud.application.validation.Validation;

import java.util.List;
import java.util.Locale;

final class HeaderValidator {

  private HeaderValidator() {
  }

  static void validate(final List<String> headers) {
    Validation.requireArgument(!headers.isEmpty(), "The dataset has no header row");
    Validation.requireArgument(
        headers.stream().noneMatch(value -> value.length() > 255),
        "Dataset column names cannot exceed 255 characters"
    );
    final long distinct = headers.stream()
        .map(value -> value.toLowerCase(Locale.ROOT).trim())
        .distinct()
        .count();
    Validation.requireArgument(
        distinct == headers.size(),
        "The dataset contains duplicated column names"
    );
  }
}
