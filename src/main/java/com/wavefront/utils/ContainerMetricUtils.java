package com.wavefront.utils;

import org.cloudfoundry.doppler.Envelope;

import static com.wavefront.utils.Constants.CONTAINER_PREFIX;
import static com.wavefront.utils.Constants.METRICS_NAME_SEP;
import static com.wavefront.utils.MetricUtils.getOrigin;
import static com.wavefront.utils.MetricUtils.getPcfMetricNamePrefix;

/**
 * Utils related to ContainerMetric
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class ContainerMetricUtils {
  // ContainerMetrics Utils
  public static String getMetricName(Envelope envelope, String suffix) {
    return getPcfMetricNamePrefix() + CONTAINER_PREFIX + METRICS_NAME_SEP + getOrigin(envelope) + METRICS_NAME_SEP + suffix;
  }
}
