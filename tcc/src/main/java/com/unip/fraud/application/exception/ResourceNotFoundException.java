package com.unip.fraud.application.exception;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(final String message) {
    super(message);
  }
}
