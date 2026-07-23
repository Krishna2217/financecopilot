package com.microsoft.financecopilot.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates exceptions into {@link ProblemDetail} responses so stack traces never reach clients.
 * Bean Validation failures include a {@code fieldErrors} map for the field name.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setTitle("Validation Failed");
    problem.setProperty("fieldErrors", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    Map<String, String> fieldErrors =
        ex.getConstraintViolations().stream()
            .collect(
                Collectors.toMap(
                    this::lastPathSegment,
                    ConstraintViolation::getMessage,
                    (first, second) -> first,
                    LinkedHashMap::new));

    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setTitle("Validation Failed");
    problem.setProperty("fieldErrors", fieldErrors);
    return problem;
  }

  private String lastPathSegment(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastDot = path.lastIndexOf('.');
    return lastDot >= 0 ? path.substring(lastDot + 1) : path;
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setTitle("Not Found");
    return problem;
  }

  @ExceptionHandler(PromptGuardViolationException.class)
  public ProblemDetail handlePromptGuardViolation(PromptGuardViolationException ex) {
    log.warn("Prompt guard rejected request: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Prompt Rejected");
    return problem;
  }

  @ExceptionHandler(SqlSafetyViolationException.class)
  public ProblemDetail handleSqlSafetyViolation(SqlSafetyViolationException ex) {
    log.warn("SQL safety validator rejected generated SQL: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setTitle("Unsafe SQL Rejected");
    return problem;
  }

  @ExceptionHandler(SqlExecutionException.class)
  public ProblemDetail handleSqlExecutionFailure(SqlExecutionException ex) {
    log.error("SQL execution failed: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
    problem.setTitle("SQL Execution Failed");
    return problem;
  }

  @ExceptionHandler(TransientAiException.class)
  public ProblemDetail handleTransientAiFailure(TransientAiException ex) {
    log.warn("Transient upstream AI provider failure: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "The AI provider is temporarily unavailable; please retry.");
    problem.setTitle("Upstream AI Provider Unavailable");
    return problem;
  }

  @ExceptionHandler(NonTransientAiException.class)
  public ProblemDetail handleNonTransientAiFailure(NonTransientAiException ex) {
    log.error("Non-transient upstream AI provider failure: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_GATEWAY, "The AI provider rejected the request.");
    problem.setTitle("Upstream AI Provider Error");
    return problem;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setTitle("Internal Server Error");
    return problem;
  }
}
