package com.wavefront.utils;

import org.cloudfoundry.doppler.Envelope;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.wavefront.utils.Constants.*;
import static org.cloudfoundry.doppler.EventType.CONTAINER_METRIC;

/**
 * Wavefront Metric Utils
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class MetricUtils {

  private static String INET_ADDR_LOCAL_HOST_NAME;

  static {
    try {
      INET_ADDR_LOCAL_HOST_NAME = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      INET_ADDR_LOCAL_HOST_NAME = "unknown";
    }
  }

  public static long getTimestamp(Envelope envelope) {
    return envelope.getTimestamp();
  }

  public static String getSource(Envelope envelope) {
    // TODO - this might change ...
    // Precedence order - { IP -> Job -> InetAddress.getLocalHost().getHostName() }
    if (!isBlank(envelope.getIp())) {
      return envelope.getIp();
    }

    if (!isBlank(envelope.getJob())) {
      return envelope.getJob();
    }

    return INET_ADDR_LOCAL_HOST_NAME;
  }

  public static String getPcfMetricNamePrefix() {
    return PCF_PREFIX + METRICS_NAME_SEP;
  }

  public static String getOrigin(Envelope envelope) {
    // Note - Don't invoke envelope.getOrigin elsewhere in the code
    // because in future we might convert origin to lower_case
    return envelope.getOrigin();
  }

  public static Map<String, String> getTags(Envelope envelope) {
    Map<String, String> map = new HashMap<>();

    if (envelope.getDeployment() != null && envelope.getDeployment().length() > 0) {
      map.put(DEPLOYMENT, Objects.toString(envelope.getDeployment()));
    }
    if (envelope.getJob() != null && envelope.getJob().length() > 0) {
      map.put(JOB, Objects.toString(envelope.getJob()));
    }

    if (envelope.getEventType().equals(CONTAINER_METRIC)) {
      /**
       * The instanceIndex of the contained application
       * along with applicationId, should uniquely identify a container.
       */
      map.put(APPLICATION_ID, envelope.getContainerMetric().getApplicationId());
      map.put(INSTANCE_INDEX, String.valueOf(envelope.getContainerMetric().getInstanceIndex().toString()));
    }
    // Add all pre-existing PCF envelope tags ...
    map.putAll(envelope.getTags());
    return map;
  }

  private static boolean isBlank(@Nullable String s) {
    if (s == null || s.isEmpty()) {
      return true;
    }
    for (int i = 0; i < s.length(); i++) {
      if (!Character.isWhitespace(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }
}
