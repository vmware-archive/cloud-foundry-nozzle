package com.wavefront.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.wavefront.integrations.metrics.WavefrontReporter;
import com.wavefront.props.WavefrontProxyProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.SystemPublicMetrics;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import static com.wavefront.utils.Constants.APP_METRICS_PREFIX;
import static com.wavefront.utils.Constants.METRICS_NAME_SEP;
import static com.wavefront.utils.Constants.PCF_PREFIX;
import static com.wavefront.utils.Constants.WAVEFRONT_FIREHOSE_NOZZLE;

/**
 * Service to report metrics to Wavefront Proxy
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Component
public class WavefrontMetricsReporter implements MetricsReporter {

  @Autowired
  private WavefrontProxyProperties proxyProperties;

  @Autowired
  private MetricRegistry metricRegistry;

  @Autowired
  private SystemPublicMetrics systemPublicMetrics;

  @PostConstruct
  public void init() {
    // add the system metrics, which needs to be forwarded to wavefront, to metricRegistry
    addMetrics(systemPublicMetrics);

    // send all the metrics registered in metricRegistry to wavefront
    WavefrontReporter wfReporter = WavefrontReporter.forRegistry(metricRegistry).
        withSource(WAVEFRONT_FIREHOSE_NOZZLE).prefixedWith(PCF_PREFIX).
        build(proxyProperties.getHostname(), proxyProperties.getPort());
    wfReporter.start(10, TimeUnit.SECONDS);
  }

  private void addMetrics(PublicMetrics metrics) {
    metricRegistry.register(WAVEFRONT_FIREHOSE_NOZZLE, (MetricSet) () -> {
      final Map<String, Metric> metricsMap = new HashMap<>();

      for (final org.springframework.boot.actuate.metrics.Metric<?> metric : metrics.metrics()) {
        metricsMap.put(metric.getName(), (Gauge<Object>) () -> metric.getValue());
      }
      return Collections.unmodifiableMap(metricsMap);
    });
  }

  @Override
  public Counter registerCounter(String name) {
    return metricRegistry.counter(WAVEFRONT_FIREHOSE_NOZZLE + METRICS_NAME_SEP +
        APP_METRICS_PREFIX + METRICS_NAME_SEP + name);
  }

  @Override
  public Timer registerTimer(String name) {
    return metricRegistry.timer(WAVEFRONT_FIREHOSE_NOZZLE + METRICS_NAME_SEP +
        APP_METRICS_PREFIX + METRICS_NAME_SEP + name);
  }

  @Override
  public Meter registerMeter(String name) {
    return metricRegistry.meter(WAVEFRONT_FIREHOSE_NOZZLE + METRICS_NAME_SEP +
        APP_METRICS_PREFIX + METRICS_NAME_SEP + name);
  }
}
