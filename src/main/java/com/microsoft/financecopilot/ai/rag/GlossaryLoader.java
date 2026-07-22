package com.microsoft.financecopilot.ai.rag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * Splits {@code src/main/resources/rag/glossary.md} into one {@link Document} per {@code ##}
 * section, so retrieval can ground a single business term (MRR, burn rate, cashflow, ...) without
 * pulling in the whole glossary.
 */
@Component
public class GlossaryLoader {

  private static final String GLOSSARY_LOCATION = "classpath:/rag/glossary.md";

  public List<Document> load() {
    Resource resource = new PathMatchingResourcePatternResolver().getResource(GLOSSARY_LOCATION);
    String content;
    try {
      content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read glossary at " + GLOSSARY_LOCATION, e);
    }

    List<Document> documents = new ArrayList<>();
    String[] sections = content.split("(?m)^## ");
    for (String section : sections) {
      if (section.isBlank()) {
        continue;
      }
      String[] lines = section.split("\n", 2);
      String term = lines[0].trim();
      if (term.isEmpty()) {
        continue;
      }
      String slug =
          term.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
      documents.add(
          new Document(
              RagDocumentIds.deterministicId("glossary:" + slug),
              "## " + section.trim(),
              Map.of("type", "glossary", "term", term)));
    }
    return documents;
  }
}
