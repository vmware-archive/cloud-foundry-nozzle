package com.wavefront.proxy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;

import com.wavefront.integrations.Wavefront;
import com.wavefront.model.AppEnvelope;
import com.wavefront.props.WavefrontProxyProperties;
import com.wavefront.utils.ContainerMetricUtils;
import com.wavefront.utils.CounterEventUtils;
import com.wavefront.utils.ValueMetricUtils;

import org.cloudfoundry.doppler.Envelope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.wavefront.utils.Constants.CPU_PERCENTAGE_SUFFIX;
import static com.wavefront.utils.Constants.CUSTOM_TAG_PREFIX;
import static com.wavefront.utils.Constants.DELTA_SUFFIX;
import static com.wavefront.utils.Constants.DISK_BYTES_QUOTA_SUFFIX;
import static com.wavefront.utils.Constants.DISK_BYTES_SUFFIX;
import static com.wavefront.utils.Constants.MEMORY_BYTES_QUOTA_SUFFIX;
import static com.wavefront.utils.Constants.MEMORY_BYTES_SUFFIX;
import static com.wavefront.utils.Constants.TOTAL_SUFFIX;
import static com.wavefront.utils.MetricUtils.getSource;
import static com.wavefront.utils.MetricUtils.getTags;
import static com.wavefront.utils.MetricUtils.getTimestamp;

/**
 * ProxyFowarder implementation
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Component
public class ProxyForwarderImpl implements ProxyForwarder {

  private static final Logger logger = Logger.getLogger(
      ProxyForwarderImpl.class.getCanonicalName());
  /**
   * Log summary of numMetrics sent every 5 seconds
   */
  private final RateLimiter summaryLogger = RateLimiter.create(0.2);
  private final AtomicLong numMetrics = new AtomicLong(0);
  private final Wavefront wavefront;
  private final ImmutableMap<String, String> customTags;

  public ProxyForwarderImpl(WavefrontProxyProperties proxyProperties)
      throws IOException {
    logger.info(String.format("Forwarding PCF metrics to Wavefront proxy at %s:%s",
        proxyProperties.getHostname(), proxyProperties.getPort()));
    this.wavefront = new Wavefront(proxyProperties.getHostname(), proxyProperties.getPort());

    // Better to compute the custom tags once during init, instead of
    // doing it for every metric.send()
    // this also means "customTag.*" tags cannot be changed after the JVM is running
    ImmutableMap.Builder builder = ImmutableMap.builder();
    Enumeration<?> names = System.getProperties().propertyNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement().toString();
      if (name.startsWith(CUSTOM_TAG_PREFIX)) {
        String key = name.substring(CUSTOM_TAG_PREFIX.length());
        String value = System.getProperty(name);
        builder.put(key, value);
      }
    }

    customTags = builder.build();
  }

  @Override
  public void forward(AppEnvelope appEnvelope) {
    Envelope envelope = appEnvelope.getEnvelope();
    switch (envelope.getEventType()) {
      case VALUE_METRIC:
        // MetricName: "pcf.<origin>.<name>.<unit>"
        send(ValueMetricUtils.getMetricName(envelope), envelope.getValueMetric().value(),
            getTimestamp(envelope), getSource(envelope), getTags(appEnvelope));
        return;
      case COUNTER_EVENT:
        // MetricName: "pcf.<origin>.<name>.total"
        send(CounterEventUtils.getMetricName(envelope, TOTAL_SUFFIX),
            envelope.getCounterEvent().getTotal(),
            getTimestamp(envelope), getSource(envelope), getTags(appEnvelope));
        // MetricName: "pcf.<origin>.<name>.delta"
        send(CounterEventUtils.getMetricName(envelope, DELTA_SUFFIX),
            envelope.getCounterEvent().getDelta(),
            getTimestamp(envelope), getSource(envelope), getTags(appEnvelope));
        return;
      case CONTAINER_METRIC:
        // MetricName: "pcf.container.<origin>.cpu_percentage"
        send(ContainerMetricUtils.getMetricName(envelope, CPU_PERCENTAGE_SUFFIX),
            envelope.getContainerMetric().getCpuPercentage(), getTimestamp(envelope),
            getSource(envelope), getTags(appEnvelope));
        // MetricName: "pcf.container.<origin>.disk_bytes"
        send(ContainerMetricUtils.getMetricName(envelope, DISK_BYTES_SUFFIX),
            envelope.getContainerMetric().getDiskBytes(), getTimestamp(envelope),
            getSource(envelope), getTags(appEnvelope));
        // MetricName: "pcf.container.<origin>.disk_bytes_quota"
        send(ContainerMetricUtils.getMetricName(envelope, DISK_BYTES_QUOTA_SUFFIX),
            envelope.getContainerMetric().getDiskBytesQuota(), getTimestamp(envelope),
            getSource(envelope), getTags(appEnvelope));
        // MetricName: "pcf.container.<origin>.memory_bytes"
        send(ContainerMetricUtils.getMetricName(envelope, MEMORY_BYTES_SUFFIX),
            envelope.getContainerMetric().getMemoryBytes(), getTimestamp(envelope),
            getSource(envelope), getTags(appEnvelope));
        // MetricName: "pcf.container.<origin>.memory_bytes_quota"
        send(ContainerMetricUtils.getMetricName(envelope, MEMORY_BYTES_QUOTA_SUFFIX),
            envelope.getContainerMetric().getMemoryBytesQuota(), getTimestamp(envelope),
            getSource(envelope), getTags(appEnvelope));
        return;
      case ERROR:
      case HTTP_START_STOP:
      case LOG_MESSAGE:
        // TODO - add support for these in future releases ...
        return;
    }
  }

  private void send(String metricName, double metricValue, Long timestamp, String source,
                    Map<String, String> tags) {
    // Add custom tags to the point metrics
    tags.putAll(customTags);
    try {
      // The if else if condition is not needed with the latest proxy but
      // what if some customer is running Wavefront PCF nozzle with an older proxy ??
      int len = timestamp.toString().length();
      if (len == 19) {
        // nanoseconds -> convert to milliseconds
        timestamp /= 1000000;
      } else if (len == 16) {
        // microseconds -> convert to milliseconds
        timestamp /= 1000;
      }
      numMetrics.incrementAndGet();
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Sending metric:" + metricName + " at timestamp: " + timestamp);
      }
      if (summaryLogger.tryAcquire()) {
        logger.info("Total number of metrics sent: " + numMetrics);
      }
      wavefront.send(metricName, metricValue, timestamp, source, tags);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Can't send data to Wavefront proxy!", e);
    }
  }
}
