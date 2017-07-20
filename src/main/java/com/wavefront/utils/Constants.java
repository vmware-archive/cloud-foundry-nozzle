package com.wavefront.utils;

/**
 * Class that defines all the constants
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class Constants {
  public static final String PCF_PREFIX = "pcf.";

  // Wavefront Metric constants
  public static final String METRICS_NAME_SEP = ".";

  // CounterEvent suffix
  public static final String TOTAL_SUFFIX = "total";
  public static final String DELTA_SUFFIX = "delta";

  // ContainerMetric suffix
  public static final String CPU_PERCENTAGE_SUFFIX = "cpuPercentage";
  public static final String DISK_BYTES_SUFFIX = "diskBytes";
  public static final String MEMORY_BYTES_SUFFIX = "memoryBytes";

  // Tag Keys
  public static final String DEPLOYMENT = "deployment";
  public static final String JOB = "job";
  // TODO - make sure we are still passing eventType as tag
  public static final String EVENT_TYPE = "eventType";
}
