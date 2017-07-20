package com.wavefront.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PCF properties getters and setters
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@ConfigurationProperties(prefix="pcf")
public class PcfProperties {

  private String user;
  private String password;
  private String host;
  private boolean skipSslValidation = true;

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public boolean isSkipSslValidation() {
    return skipSslValidation;
  }

  public void setSkipSslValidation(boolean skipSslValidation) {
    this.skipSslValidation = skipSslValidation;
  }
}
