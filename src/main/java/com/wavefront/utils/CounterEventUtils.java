package com.wavefront.utils;

import org.cloudfoundry.doppler.Envelope;

import static com.wavefront.utils.Constants.METRICS_NAME_SEP;
import static com.wavefront.utils.MetricUtils.getOrigin;
import static com.wavefront.utils.MetricUtils.getPcfMetricNamePrefix;

/**
 * Utils related to CounterEvent
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class CounterEventUtils {
  public static String getMetricName(Envelope envelope, String suffix) {
    return getPcfMetricNamePrefix() + getOrigin(envelope) + METRICS_NAME_SEP +
            envelope.getCounterEvent().getName() + METRICS_NAME_SEP + suffix;
  }
}
