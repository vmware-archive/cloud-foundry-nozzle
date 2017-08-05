package com.wavefront.service;

import org.springframework.stereotype.Service;

/**
 * Service class responsible for connecting to a firehose and
 * forwarding event envelope via nozzle to Wavefront Proxy
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Service
public interface FirehoseToWavefrontProxyConnector {
  /**
   * Connect Nozzle to PCF firehose
   */
  void connect();
}
