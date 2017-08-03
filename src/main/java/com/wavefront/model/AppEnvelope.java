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
}
