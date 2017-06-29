package com.netflix.kayenta.r;

public class RExecutionException extends Exception {
  public RExecutionException(String message) {
    super(message);
  }

  public RExecutionException(String message, Throwable t) {
    super(message, t);
  }
}
