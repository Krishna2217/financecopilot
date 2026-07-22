package com.microsoft.financecopilot.query.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(
    example =
        """
        {
          "generatedSql": "SELECT sum(amount) FROM analytics.transactions t JOIN analytics.categories c ON c.id = t.category_id WHERE c.name = 'Groceries' AND t.transaction_date >= date_trunc('month', now() - interval '1 month') LIMIT 1000",
          "rationale": "Sums transaction amounts for the Groceries category over the last calendar month.",
          "columns": ["sum"],
          "rows": [{"sum": -312.47}],
          "summary": "You spent $312.47 on groceries last month.",
          "historyId": 42
        }
        """)
public record QueryResponse(
    String generatedSql,
    String rationale,
    List<String> columns,
    List<Map<String, Object>> rows,
    String summary,
    Long historyId) {}
