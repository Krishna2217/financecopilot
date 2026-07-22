package com.microsoft.financecopilot.common.exception;

/**
 * Thrown by {@code SqlExecutor} when already-validated SQL fails at execution time (statement
 * timeout, connection failure). Distinct from {@link SqlSafetyViolationException}, which is only
 * for {@code SqlSafetyValidator} rejecting SQL before it is ever executed.
 */
public class SqlExecutionException extends RuntimeException {

  public SqlExecutionException(String message) {
    super(message);
  }
}
