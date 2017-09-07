package com.wavefront.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PCF AppInfo properties getters and setters
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@ConfigurationProperties(prefix = "pcf.appInfo")
public class AppInfoProperties {
  /**
   * Whether to fetch PCF app info or not
   */
  private boolean fetchAppInfo;

  /**
   * AppInfo is cached. Tune the cache size using this variable
   */
  private int appInfoCacheSize;

  /**
   * Entries in cache expire after write. Tune this interval using this variable
   */
  private int cacheExpireIntervalHours;

  public boolean isFetchAppInfo() {
    return fetchAppInfo;
  }

  public void setFetchAppInfo(boolean fetchAppInfo) {
    this.fetchAppInfo = fetchAppInfo;
  }

  public int getAppInfoCacheSize() {
    return appInfoCacheSize;
  }

  public void setAppInfoCacheSize(int appInfoCacheSize) {
    this.appInfoCacheSize = appInfoCacheSize;
  }

  public int getCacheExpireIntervalHours() {
    return cacheExpireIntervalHours;
  }

  public void setCacheExpireIntervalHours(int cacheExpireIntervalHours) {
    this.cacheExpireIntervalHours = cacheExpireIntervalHours;
  }
}
