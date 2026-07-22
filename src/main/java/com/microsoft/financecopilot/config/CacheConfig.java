package com.microsoft.financecopilot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Caffeine-backed caching for the (no-AI) analytics dashboard endpoints, 5-minute TTL. */
@Configuration
@EnableCaching
public class CacheConfig {

  public static final String KPIS_CACHE = "kpis";
  public static final String SPEND_BY_CATEGORY_CACHE = "spend-by-category";
  public static final String CASHFLOW_CACHE = "cashflow";
  public static final String TREND_CACHE = "trend";

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager manager =
        new CaffeineCacheManager(KPIS_CACHE, SPEND_BY_CATEGORY_CACHE, CASHFLOW_CACHE, TREND_CACHE);
    manager.setCaffeine(
        Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5)).maximumSize(500));
    return manager;
  }
}
