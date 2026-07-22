package com.microsoft.financecopilot.query;

import com.microsoft.financecopilot.query.dto.QueryHistoryItem;
import com.microsoft.financecopilot.query.dto.QueryRequest;
import com.microsoft.financecopilot.query.dto.QueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/query")
@Tag(name = "Query", description = "Natural-language querying over finance data")
@ConditionalOnProperty(
    prefix = "app.ai",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class QueryController {

  private final QueryService queryService;

  public QueryController(QueryService queryService) {
    this.queryService = queryService;
  }

  @PostMapping
  @Operation(
      summary =
          "Translate a natural-language question into SQL, execute it, and summarize the result")
  public QueryResponse query(@Valid @RequestBody QueryRequest request) {
    return queryService.executeQuery(request.naturalLanguageQuery());
  }

  @GetMapping("/history")
  @Operation(summary = "Paginated query history, most recent first")
  public Page<QueryHistoryItem> history(Pageable pageable) {
    return queryService.getHistory(pageable);
  }
}
