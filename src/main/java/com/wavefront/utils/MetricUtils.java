package com.wavefront.utils;

import com.google.common.collect.Maps;

import com.wavefront.model.AppEnvelope;
import com.wavefront.model.AppInfo;

import org.cloudfoundry.doppler.Envelope;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import static com.wavefront.utils.Constants.APPLICATION_ID;
import static com.wavefront.utils.Constants.APPLICATION_NAME;
import static com.wavefront.utils.Constants.DEPLOYMENT;
import static com.wavefront.utils.Constants.INSTANCE_INDEX;
import static com.wavefront.utils.Constants.JOB;
import static com.wavefront.utils.Constants.METRICS_NAME_SEP;
import static com.wavefront.utils.Constants.ORG;
import static com.wavefront.utils.Constants.PCF_PREFIX;
import static com.wavefront.utils.Constants.SPACE;
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
    // envelope.getTimestamp() marked as @Nullable
    if (envelope.getTimestamp() == null) {
      return System.currentTimeMillis();
    } else {
      return envelope.getTimestamp();
    }
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

  public static Map<String, String> getTags(AppEnvelope appEnvelope) {
    Envelope envelope = appEnvelope.getEnvelope();
    Map<String, String> map = Maps.newHashMap();

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
      String applicationId = envelope.getContainerMetric().getApplicationId();
      map.put(APPLICATION_ID, applicationId);
      map.put(INSTANCE_INDEX, String.valueOf(
          envelope.getContainerMetric().getInstanceIndex().toString()));
    }

    /**
     * Add AppInfo tags for superior querying (better than applicationId GUIDs)
     * Only applicable to CONTAINER_METRIC, HTTP_START_STOP and LOG_MESSAGE event_types
     */
    Optional<AppInfo> optionalAppInfo = appEnvelope.getAppInfo();
    if (optionalAppInfo.isPresent()) {
      AppInfo appInfo = optionalAppInfo.get();
      String applicationName = appInfo.getApplicationName();
      if (applicationName != null && applicationName.length() > 0) {
        map.put(APPLICATION_NAME, applicationName);
      }
      String org = appInfo.getOrg();
      if (org != null && org.length() > 0) {
        map.put(ORG, org);
      }
      String space = appInfo.getSpace();
      if (space != null && space.length() > 0) {
        map.put(SPACE, space);
      }
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
