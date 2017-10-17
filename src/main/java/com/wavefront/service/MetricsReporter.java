package com.wavefront.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import org.springframework.stereotype.Service;

/**
 * Service to report metrics
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Service
public interface MetricsReporter {

  /**
   * Register Counter with Metric Registry
   *
   * @param name Name of the counter
   * @return Counter
   */
  Counter registerCounter(String name);

  /**
   * Register Timer with Metric Registry
   *
   * @param name Name of the timer
   * @return Timer
   */
  Timer registerTimer(String name);

  /**
   * Register Meter with Metric Registry
   *
   * @param name Name of the meter
   * @return Meter
   */
  Meter registerMeter(String name);

}
