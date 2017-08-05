package com.wavefront.utils;

import org.cloudfoundry.doppler.Envelope;

import static com.wavefront.utils.Constants.METRICS_NAME_SEP;
import static com.wavefront.utils.MetricUtils.getOrigin;
import static com.wavefront.utils.MetricUtils.getPcfMetricNamePrefix;

/**
 * Utils related to ValueMetric
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class ValueMetricUtils {
  public static String getMetricName(Envelope envelope) {
    return getPcfMetricNamePrefix() + getOrigin(envelope) + METRICS_NAME_SEP + envelope.getValueMetric().getName() +
            METRICS_NAME_SEP + envelope.getValueMetric().getUnit();
  }
}
