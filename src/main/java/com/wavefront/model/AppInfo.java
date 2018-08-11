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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || !(o instanceof AppInfo)) {
      return false;
    }

    AppInfo other = (AppInfo) o;
    return applicationName.equals(other.applicationName) && org.equals(other.org) &&
        space.equals(other.space);
  }

  @Override
  public int hashCode() {
    return 17 * applicationName.hashCode() + 31 * org.hashCode() + 53 * space.hashCode();
  }
}
