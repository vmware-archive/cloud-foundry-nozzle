package com.wavefront.proxy;

import org.cloudfoundry.doppler.Envelope;

/**
 * Converts PCF Envelope-Event into metrics and then forward those metrics to Wavefront Proxy
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public interface ProxyForwarder {

  /**
   * Forward metrics generated from an envelope to the proxy
   *
   * @param envelope
   */
  void forward(Envelope envelope);
}
