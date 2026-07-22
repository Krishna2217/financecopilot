package com.microsoft.financecopilot.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsPromptGuardViolationTo400WithoutStackTrace() {
    ProblemDetail problem =
        handler.handlePromptGuardViolation(new PromptGuardViolationException("bad prompt"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).isEqualTo("bad prompt");
    assertThat(problem.getTitle()).isEqualTo("Prompt Rejected");
  }

  @Test
  void mapsSqlSafetyViolationTo400WithoutStackTrace() {
    ProblemDetail problem =
        handler.handleSqlSafetyViolation(new SqlSafetyViolationException("unsafe sql"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).isEqualTo("unsafe sql");
  }

  @Test
  void mapsUnexpectedExceptionsTo500WithoutLeakingDetails() {
    ProblemDetail problem = handler.handleUnexpected(new RuntimeException("db password is x"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getDetail()).doesNotContain("db password is x");
  }

  @Test
  void mapsSqlExecutionFailureToGatewayTimeout() {
    ProblemDetail problem =
        handler.handleSqlExecutionFailure(new SqlExecutionException("statement timeout"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT.value());
    assertThat(problem.getDetail()).isEqualTo("statement timeout");
  }

  @Test
  void mapsTransientAiFailureToServiceUnavailableWithoutLeakingUpstreamDetails() {
    ProblemDetail problem =
        handler.handleTransientAiFailure(new TransientAiException("HTTP 429 Too Many Requests"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    assertThat(problem.getDetail()).doesNotContain("429");
  }

  @Test
  void mapsNonTransientAiFailureToBadGateway() {
    ProblemDetail problem =
        handler.handleNonTransientAiFailure(new NonTransientAiException("invalid api key"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
  }

  @Test
  void mapsQueryParamConstraintViolationsTo400WithFieldErrors() {
    RequestParams params = new RequestParams();
    params.months = 0;
    var violations = VALIDATOR.validate(params);

    ProblemDetail problem =
        handler.handleConstraintViolation(new ConstraintViolationException(violations));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getTitle()).isEqualTo("Validation Failed");
    @SuppressWarnings("unchecked")
    Map<String, String> fieldErrors =
        (Map<String, String>) problem.getProperties().get("fieldErrors");
    assertThat(fieldErrors).containsKey("months");
  }

  private static class RequestParams {
    @Min(1)
    int months;
  }
}
