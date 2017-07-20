package com.wavefront.proxy;

import com.wavefront.integrations.Wavefront;
import com.wavefront.props.WavefrontProxyProperties;
import com.wavefront.utils.ContainerMetricUtils;
import com.wavefront.utils.CounterEventUtils;
import com.wavefront.utils.ValueMetricUtils;
import org.cloudfoundry.doppler.Envelope;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import static com.wavefront.utils.Constants.*;
import static com.wavefront.utils.MetricUtils.*;

/**
 * ProxyFowarder implementation
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class ProxyForwarderImpl implements ProxyForwarder {

  protected static final Logger logger = Logger.getLogger(ProxyForwarderImpl.class.getCanonicalName());

  private final Wavefront wavefront;

  public ProxyForwarderImpl(WavefrontProxyProperties proxyProperties) throws IOException {
    wavefront = new Wavefront(proxyProperties.getHostname(), proxyProperties.getPort());
  }

  @Override
  public void forward(Envelope envelope) {
    switch (envelope.getEventType()) {
      case VALUE_METRIC:
        // Format: "pcf.origin.<name>.<unit>"
        send(ValueMetricUtils.getMetricName(envelope), envelope.getValueMetric().value(),
                getTimestamp(envelope), getSource(envelope), getTags(envelope));
        return;
      case COUNTER_EVENT:
        // Format: "pcf.origin.<name>.total"
        send(CounterEventUtils.getMetricName(envelope, TOTAL_SUFFIX), envelope.getCounterEvent().getTotal(),
                getTimestamp(envelope), getSource(envelope), getTags(envelope));
        // Format: "pcf.origin.<name>.delta"
        send(CounterEventUtils.getMetricName(envelope, DELTA_SUFFIX), envelope.getCounterEvent().getDelta(),
                getTimestamp(envelope), getSource(envelope), getTags(envelope));
        return;
      case CONTAINER_METRIC:
        /**
         * TODO - revisit the container metric format ...
         *
         * Example: Right now below container metric
         *
         * origin:"rep" eventType:ContainerMetric timestamp:1500418763082458372 deployment:"cf" job:"diego_cell"
         * index:"7b4512eb-a99c-435d-9c65-878b60fe29fe" ip:"10.202.5.16"
         * containerMetric:<applicationId:"957a4593-28da-43a1-92f6-50cffd75eca9" instanceIndex:1
         * cpuPercentage:0.2728349571783102 memoryBytes:667291648 diskBytes:182566912 6:1073741824 7:1073741824 >
         *
         * is transformed into
         *
         * "pcf.rep.957a4593-28da-43a1-92f6-50cffd75eca9.1.cpuPercentage 0.2728349571783102 1500418763082458372
         * source=10.202.5.16 eventType=CONTAINER_METRIC job=diego_cell deployment=cf"
         *
         */
        send(ContainerMetricUtils.getMetricName(envelope, CPU_PERCENTAGE_SUFFIX),
                envelope.getContainerMetric().getCpuPercentage(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        send(ContainerMetricUtils.getMetricName(envelope, DISK_BYTES_SUFFIX),
                envelope.getContainerMetric().getDiskBytes(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        // Note: diskBytesQuota is not a metric
        send(ContainerMetricUtils.getMetricName(envelope, MEMORY_BYTES_SUFFIX),
                envelope.getContainerMetric().getMemoryBytes(), getTimestamp(envelope),
                getSource(envelope), getTags(envelope));
        // Note: memoryBytesQuota is not a metric
        // TODO - instead of sending disk_bytes and memory_bytes,
        // send the ratio of (memory_bytes/memory_bytes_quota)
        // and (disk_bytes/disk_bytes_quota) ??
        return;
      case ERROR:
        // TODO
        return;
      case HTTP_START_STOP:
        // TODO
        return;
      case LOG_MESSAGE:
        // TODO
        return;
    }
  }

  private void send(String metricName, double metricValue, Long timestamp, String source, Map<String, String> tags) {
    try {
      logger.info("Sending metric:" + metricName);
      wavefront.send(metricName, metricValue, timestamp, source, tags);
    } catch (IOException e) {
      logger.warning("Can't send data to Wavefront proxy!");
    }
  }
}
