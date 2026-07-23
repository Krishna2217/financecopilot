package com.microsoft.financecopilot.report.dto;

import com.microsoft.financecopilot.ai.dto.ReportSection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.YearMonth;
import java.util.List;

@Schema(
    example =
        """
        {
          "month": "2026-06",
          "markdown": "# Executive Summary\\n...",
          "sections": [
            {"title": "Summary", "content": "..."},
            {"title": "Key Metrics", "content": "..."}
          ],
          "cached": false
        }
        """)
public record ExecutiveReportResponse(
    YearMonth month, String markdown, List<ReportSection> sections, boolean cached) {}
