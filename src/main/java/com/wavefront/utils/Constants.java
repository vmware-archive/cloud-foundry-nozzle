package com.wavefront.utils;

/**
 * Class that defines all the constants
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class Constants {
  public static final String WAVEFRONT_FIREHOSE_NOZZLE = "wavefront-firehose-nozzle";
  public static final String PCF_PREFIX = "pcf";

  // Wavefront Metric constants
  public static final String METRICS_NAME_SEP = ".";

  // CounterEvent suffix
  public static final String TOTAL_SUFFIX = "total";
  public static final String DELTA_SUFFIX = "delta";

  // ContainerMetric suffix
  public static final String CONTAINER_PREFIX = "container";
  public static final String CPU_PERCENTAGE_SUFFIX = "cpu_percentage";
  public static final String DISK_BYTES_SUFFIX = "disk_bytes";
  public static final String DISK_BYTES_QUOTA_SUFFIX = "disk_bytes_quota";
  public static final String MEMORY_BYTES_SUFFIX = "memory_bytes";
  public static final String MEMORY_BYTES_QUOTA_SUFFIX = "memory_bytes_quota";

  // Tag Keys
  public static final String APPLICATION_NAME = "applicationName";
  public static final String ORG = "org";
  public static final String SPACE = "space";
  public static final String APPLICATION_ID = "applicationId";
  public static final String INSTANCE_INDEX = "instanceIndex";
  public static final String DEPLOYMENT = "deployment";
  public static final String JOB = "job";
}
