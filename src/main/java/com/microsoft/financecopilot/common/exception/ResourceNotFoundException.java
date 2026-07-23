package com.microsoft.financecopilot.common.exception;

/** Thrown when a requested resource (e.g. an anomaly id) doesn't exist. */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
