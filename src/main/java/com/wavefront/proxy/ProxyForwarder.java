package com.wavefront.proxy;

import com.wavefront.model.AppEnvelope;
import org.springframework.stereotype.Component;

/**
 * Converts PCF Envelope-Event into metrics and then forward those metrics to Wavefront Proxy
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Component
public interface ProxyForwarder {

  /**
   * Forward metrics generated from an envelope to the proxy
   *
   * @param envelope Metrics envelope
   */
  void forward(AppEnvelope envelope);
}
