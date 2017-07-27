package com.wavefront.proxy;

import com.wavefront.integrations.Wavefront;
import com.wavefront.props.WavefrontProxyProperties;
import com.wavefront.utils.ContainerMetricUtils;
import com.wavefront.utils.CounterEventUtils;
import com.wavefront.utils.ValueMetricUtils;
import org.cloudfoundry.doppler.Envelope;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.wavefront.utils.Constants.*;
import static com.wavefront.utils.MetricUtils.*;

/**
 * ProxyFowarder implementation
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class ProxyForwarderImpl implements ProxyForwarder {

  private static final Logger logger = Logger.getLogger(ProxyForwarderImpl.class.getCanonicalName());

  private final Wavefront wavefront;

  public ProxyForwarderImpl(WavefrontProxyProperties proxyProperties) throws IOException {
    logger.info(String.format("Forwarding PCF metrics to Wavefront proxy at %s:%s",
            proxyProperties.getHostname(), proxyProperties.getPort()));
    wavefront = new Wavefront(proxyProperties.getHostname(), proxyProperties.getPort());
  }

  @Override
  public void forward(Envelope envelope) {
    switch (envelope.getEventType()) {
      case VALUE_METRIC:
        // MetricName: "pcf.<origin>.<name>.<unit>"
        send(ValueMetricUtils.getMetricName(envelope), envelope.getValueMetric().value(),
                getTimestamp(envelope), getSource(envelope), getTags(envelope));
        return;
      case COUNTER_EVENT:
        // MetricName: "pcf.<origin>.<name>.total"
        send(CounterEventUtils.getMetricName(envelope, TOTAL_SUFFIX), envelope.getCounterEvent().getTotal(),
                getTimestamp(envelope), getSource(envelope), getTags(envelope));
        // MetricName: "pcf.<origin>.<name>.delta"
        send(CounterEventUtils.getMetricName(envelope, DELTA_SUFFIX), envelope.getCounterEvent().getDelta(),
                getTimestamp(envelope), getSource(envelope), getTags(envelope));
        return;
      case CONTAINER_METRIC:
        // MetricName: "pcf.container.<origin>.cpu_percentage"
        send(ContainerMetricUtils.getMetricName(envelope, CPU_PERCENTAGE_SUFFIX),
                envelope.getContainerMetric().getCpuPercentage(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        // MetricName: "pcf.container.<origin>.disk_bytes"
        send(ContainerMetricUtils.getMetricName(envelope, DISK_BYTES_SUFFIX),
                envelope.getContainerMetric().getDiskBytes(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        // MetricName: "pcf.container.<origin>.disk_bytes_quota"
        send(ContainerMetricUtils.getMetricName(envelope, DISK_BYTES_QUOTA_SUFFIX),
                envelope.getContainerMetric().getDiskBytesQuota(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        // MetricName: "pcf.container.<origin>.memory_bytes"
        send(ContainerMetricUtils.getMetricName(envelope, MEMORY_BYTES_SUFFIX),
                envelope.getContainerMetric().getMemoryBytes(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        // MetricName: "pcf.container.<origin>.memory_bytes_quota"
        send(ContainerMetricUtils.getMetricName(envelope, MEMORY_BYTES_QUOTA_SUFFIX),
                envelope.getContainerMetric().getMemoryBytesQuota(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        return;
      case ERROR:
      case HTTP_START_STOP:
      case LOG_MESSAGE:
        // TODO - add support for these in future releases ...
        return;
    }
  }

  private void send(String metricName, double metricValue, Long timestamp, String source, Map<String, String> tags) {
    try {
      // The if else if condition is not needed with the latest proxy but
      // what if some customer is running Wavefront PCF nozzle with an older proxy ??
      if (timestamp.toString().length() == 19) {
        // nanoseconds -> convert to milliseconds
        timestamp /= 1000000;
      } else if (timestamp.toString().length() == 16) {
        // microseconds -> convert to milliseconds
        timestamp /= 1000;
      }
      logger.info("Sending metric:" + metricName + " at timestamp: " + timestamp);
      wavefront.send(metricName, metricValue, timestamp, source, tags);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Can't send data to Wavefront proxy!", e);
    }
  }
}
