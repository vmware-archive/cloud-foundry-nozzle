package com.wavefront.utils;

import org.cloudfoundry.doppler.Envelope;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.wavefront.utils.Constants.*;

/**
 * Wavefront Metric Utils
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class MetricUtils {
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

    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "unknown";
    }
  }

  public static Map<String, String> getTags(Envelope envelope) {
    Map<String, String> map = new HashMap<>();
    // TODO - not sure yet whether we want eventType as the tag ...
    map.put(EVENT_TYPE, Objects.toString(envelope.getEventType()));
    if (envelope.getDeployment() != null && envelope.getDeployment().length() > 0) {
      map.put(DEPLOYMENT, Objects.toString(envelope.getDeployment()));
    }
    if (envelope.getJob() != null && envelope.getJob().length() > 0) {
      map.put(JOB, Objects.toString(envelope.getJob()));
    }
    return map;
  }

  private static boolean isBlank(String s) {
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
