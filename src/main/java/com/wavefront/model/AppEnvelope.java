package com.wavefront.model;

import org.cloudfoundry.doppler.Envelope;

import java.util.Optional;

/**
 * Event data enhanced with PCF application details
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class AppEnvelope {
  private final Envelope envelope;
  private final Optional<AppInfo> appInfo;

  public AppEnvelope(Envelope envelope, Optional<AppInfo> appInfo) {
    this.envelope = envelope;
    this.appInfo = appInfo;
  }

  public Envelope getEnvelope() {
    return envelope;
  }

  public Optional<AppInfo> getAppInfo() {
    return appInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof AppEnvelope)) {
      return false;
    }

    AppEnvelope other = (AppEnvelope) o;

    if (!this.envelope.equals(other.envelope)) {
      return false;
    }

    if (!this.appInfo.isPresent() && !other.appInfo.isPresent()) {
      return true;
    }

    if (!this.appInfo.isPresent() || !other.appInfo.isPresent()) {
      return false;
    }

    return this.appInfo.get().equals(other.appInfo.get());
  }

  @Override
  public int hashCode() {
    int result = 17 * envelope.hashCode();
    if (appInfo.isPresent()) {
      result += appInfo.get().hashCode();
    }
    return result;
  }
}
