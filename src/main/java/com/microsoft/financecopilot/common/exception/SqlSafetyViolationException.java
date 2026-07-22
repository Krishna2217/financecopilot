package com.microsoft.financecopilot.common.exception;

/** Thrown by {@code SqlSafetyValidator} when AI-generated SQL fails a safety check. */
public class SqlSafetyViolationException extends RuntimeException {

  public SqlSafetyViolationException(String message) {
    super(message);
  }
}
