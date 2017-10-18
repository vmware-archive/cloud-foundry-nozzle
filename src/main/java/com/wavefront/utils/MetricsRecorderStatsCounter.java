package com.wavefront.utils;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import com.wavefront.service.MetricsReporter;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import static com.wavefront.utils.Constants.CAFFEINE_PREFIX;
import static com.wavefront.utils.Constants.METRICS_NAME_SEP;

/**
 * A push-based approach that uses a custom StatsCounter
 * so that the metrics are updated directly during the cache operations.
 *
 * See https://github.com/ben-manes/caffeine/wiki/Statistics for more details
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class MetricsRecorderStatsCounter implements StatsCounter {
  private final Meter hitCount;
  private final Meter missCount;
  private final Meter loadSuccessCount;
  private final Meter loadFailureCount;
  private final Timer totalLoadTime;
  private final Meter evictionCount;
  private final Meter evictionWeight;

  public MetricsRecorderStatsCounter(MetricsReporter metricsReporter) {
    final String metricsPrefix = CAFFEINE_PREFIX + METRICS_NAME_SEP;
    hitCount = metricsReporter.registerMeter(metricsPrefix + "hits");
    missCount = metricsReporter.registerMeter(metricsPrefix + "misses");
    totalLoadTime = metricsReporter.registerTimer(metricsPrefix + "loads");
    loadSuccessCount = metricsReporter.registerMeter(metricsPrefix + "loads-success");
    loadFailureCount = metricsReporter.registerMeter(metricsPrefix + "loads-failure");
    evictionCount = metricsReporter.registerMeter(metricsPrefix + "evictions");
    evictionWeight = metricsReporter.registerMeter(metricsPrefix + "evictions-weight");
  }

  @Override
  public void recordHits(int count) {
    hitCount.mark(count);
  }

  @Override
  public void recordMisses(int count) {
    missCount.mark(count);
  }

  @Override
  public void recordLoadSuccess(long loadTime) {
    loadSuccessCount.mark();
    totalLoadTime.update(loadTime, TimeUnit.NANOSECONDS);
  }

  @Override
  public void recordLoadFailure(long loadTime) {
    loadFailureCount.mark();
    totalLoadTime.update(loadTime, TimeUnit.NANOSECONDS);
  }

  @Override
  public void recordEviction() {
    // This method is scheduled for removal in version 3.0 in favor of recordEviction(weight)
    recordEviction(1);
  }

  @Override
  public void recordEviction(int weight) {
    evictionCount.mark();
    evictionWeight.mark(weight);
  }

  @Nonnull
  @Override
  public CacheStats snapshot() {
    return new CacheStats(
        hitCount.getCount(),
        missCount.getCount(),
        loadSuccessCount.getCount(),
        loadFailureCount.getCount(),
        totalLoadTime.getCount(),
        evictionCount.getCount(),
        evictionWeight.getCount());
  }

  @Override
  public String toString() {
    return snapshot().toString();
  }
}
