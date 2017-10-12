package com.wavefront.service;

import com.codahale.metrics.Counter;

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
   * @param name
   * @return
   */
  Counter registerCounter(String name);
}
