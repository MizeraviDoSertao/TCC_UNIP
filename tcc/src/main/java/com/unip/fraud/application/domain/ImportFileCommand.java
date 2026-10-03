package com.unip.fraud.application.domain;

import java.io.IOException;
import java.io.InputStream;

public record ImportFileCommand(
    String fileName,
    long size,
    InputStreamSource content
) {
  @FunctionalInterface
  public interface InputStreamSource {
    InputStream open() throws IOException;
  }
}
