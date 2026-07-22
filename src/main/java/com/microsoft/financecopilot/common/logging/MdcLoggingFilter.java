package com.microsoft.financecopilot.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates {@code traceId}/{@code requestId} into MDC for the lifetime of a request so every log
 * line (and the logging pattern in application.yml) can correlate to it. {@code requestId} is taken
 * from the {@code X-Request-Id} header when present so callers can trace their own requests end to
 * end; otherwise one is generated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String MDC_TRACE_ID = "traceId";
  public static final String MDC_REQUEST_ID = "requestId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    String traceId = UUID.randomUUID().toString();

    MDC.put(MDC_REQUEST_ID, requestId);
    MDC.put(MDC_TRACE_ID, traceId);
    response.setHeader(REQUEST_ID_HEADER, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_REQUEST_ID);
      MDC.remove(MDC_TRACE_ID);
    }
  }
}
