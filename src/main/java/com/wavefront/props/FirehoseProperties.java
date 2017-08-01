package com.wavefront.props;

import org.cloudfoundry.doppler.EventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * PCF Firehose properties getters and setters
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@ConfigurationProperties(prefix = "pcf.firehose")
public class FirehoseProperties {
  private List<EventType> eventTypes;
  private String subscriptionId;
  /**
   * By default 4 threads sending metrics to proxy
   */
  private int parallelism = 4;

  public List<EventType> getEventTypes() {
    return eventTypes;
  }

  public void setEventTypes(List<EventType> eventTypes) {
    this.eventTypes = eventTypes;
  }

  public String getSubscriptionId() {
    return subscriptionId;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public int getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }
}
