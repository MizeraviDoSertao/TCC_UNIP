package com.unip.fraud.application.validation;

import java.util.function.Supplier;

public final class Validation {

  public static void require(final boolean condition, final Supplier<? extends RuntimeException> exceptionSupplier) {
    if (!condition) {
      throw exceptionSupplier.get();
    }
  }

  public static void requireArgument(final boolean condition, final String message) {
    require(condition, () -> new IllegalArgumentException(message));
  }
}
