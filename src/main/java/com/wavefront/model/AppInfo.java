package com.wavefront.model;

/**
 * PCF Application Details
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class AppInfo {
  private final String applicationName;
  private final String org;
  private final String space;

  public AppInfo(String applicationName, String org, String space) {
    this.applicationName = applicationName;
    this.org = org;
    this.space = space;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public String getOrg() {
    return org;
  }

  public String getSpace() {
    return space;
  }

}
