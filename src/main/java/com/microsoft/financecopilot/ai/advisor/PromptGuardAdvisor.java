package com.microsoft.financecopilot.ai.advisor;

import com.microsoft.financecopilot.common.exception.PromptGuardViolationException;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;

/**
 * Rejects user prompts that look like an attempt to have the model emit DDL/DML, or that look like
 * a prompt-injection attempt, before the request ever reaches the model. This is a coarse first
 * line of defense on the natural-language input; {@code SqlSafetyValidator} is the authoritative
 * check on the model's SQL *output* and is not implemented as an advisor.
 */
public class PromptGuardAdvisor implements BaseAdvisor {

  private static final Pattern FORBIDDEN_SQL_TOKENS =
      Pattern.compile(
          "\\b(DROP|DELETE|UPDATE|INSERT|ALTER|TRUNCATE|GRANT|REVOKE|CREATE|EXEC|EXECUTE|CALL|COPY|MERGE)\\b",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern INJECTION_PATTERNS =
      Pattern.compile(
          "ignore (all |any )?(previous|prior|above) instructions"
              + "|disregard (all |any )?(previous|prior|above)"
              + "|you are now"
              + "|reveal (your |the )?system prompt"
              + "|act as (if )?(you (are|were) )?(a|an) unrestricted",
          Pattern.CASE_INSENSITIVE);

  @Override
  public String getName() {
    return "PromptGuardAdvisor";
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
    String userText = request.prompt().getContents();
    if (FORBIDDEN_SQL_TOKENS.matcher(userText).find()) {
      throw new PromptGuardViolationException(
          "Prompt rejected: contains a DDL/DML-like token that is not permitted in a request");
    }
    if (INJECTION_PATTERNS.matcher(userText).find()) {
      throw new PromptGuardViolationException(
          "Prompt rejected: matched a known prompt-injection pattern");
    }
    return request;
  }

  @Override
  public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
    return response;
  }
}
