package com.microsoft.financecopilot.ai.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads curated NL→SQL example pairs from {@code src/main/resources/rag/nl2sql-fewshots.jsonl} (one
 * JSON object per line: {@code {"nl": ..., "sql": ...}}), rendering each as a retrievable {@link
 * Document}.
 */
@Component
public class NlSqlFewShotLoader {

  private static final String FEWSHOTS_LOCATION = "classpath:/rag/nl2sql-fewshots.jsonl";

  private final ObjectMapper objectMapper;

  public NlSqlFewShotLoader(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<Document> load() {
    Resource resource = new PathMatchingResourcePatternResolver().getResource(FEWSHOTS_LOCATION);
    List<Document> documents = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      int index = 0;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        Map<?, ?> pair = objectMapper.readValue(line, Map.class);
        String nl = String.valueOf(pair.get("nl"));
        String sql = String.valueOf(pair.get("sql"));
        String content = "Example question: " + nl + "\nExample SQL: " + sql;
        documents.add(
            new Document(
                RagDocumentIds.deterministicId("fewshot:" + index),
                content,
                Map.of("type", "nl2sql-fewshot", "nl", nl)));
        index++;
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read few-shots at " + FEWSHOTS_LOCATION, e);
    }
    return documents;
  }
}
