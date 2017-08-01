package com.wavefront.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Wavefront proxy properties getters and setters
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@ConfigurationProperties(prefix = "wavefront.proxy")
public class WavefrontProxyProperties {
  private String hostname;
  private int port;

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }
}
