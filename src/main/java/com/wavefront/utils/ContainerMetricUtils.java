package com.wavefront.utils;

import org.cloudfoundry.doppler.ContainerMetric;
import org.cloudfoundry.doppler.Envelope;

import static com.wavefront.utils.Constants.METRICS_NAME_SEP;
import static com.wavefront.utils.Constants.PCF_PREFIX;

/**
 * Utils related to ContainerMetric
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class ContainerMetricUtils {
  // ContainerMetrics Utils
  public static String getMetricName(Envelope envelope, String suffix) {
    // TODO - verify if this is what we want ...
    ContainerMetric containerMetric = envelope.getContainerMetric();
    // The instance index of the contained application.
    // (This, with applicationId, should uniquely identify a container.)
    return PCF_PREFIX + envelope.getOrigin() + METRICS_NAME_SEP + containerMetric.getApplicationId()
            + METRICS_NAME_SEP + containerMetric.getInstanceIndex() + METRICS_NAME_SEP + suffix;
  }
}
