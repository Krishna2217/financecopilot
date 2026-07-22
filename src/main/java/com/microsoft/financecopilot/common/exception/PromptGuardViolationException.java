package com.microsoft.financecopilot.common.exception;

/**
 * Thrown by {@code PromptGuardAdvisor} when a user prompt looks like DDL/DML or a prompt injection
 * attempt.
 */
public class PromptGuardViolationException extends RuntimeException {

  public PromptGuardViolationException(String message) {
    super(message);
  }
}
