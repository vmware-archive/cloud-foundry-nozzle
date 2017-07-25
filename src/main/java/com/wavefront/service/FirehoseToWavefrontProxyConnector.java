package com.wavefront.service;

import com.wavefront.props.FirehoseProperties;
import com.wavefront.proxy.ProxyForwarder;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.FirehoseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import static com.wavefront.utils.Constants.WAVEFRONT_FIREHOSE_NOZZLE;

/**
 * Service class responsible for connecting to a firehose and
 * forwarding event envelope via nozzle to Wavefront Proxy
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Service
public class FirehoseToWavefrontProxyConnector {
  private final DopplerClient dopplerClient;
  private final FirehoseProperties firehoseProperties;
  private final ProxyForwarder proxyForwarder;

  @Autowired
  public FirehoseToWavefrontProxyConnector(DopplerClient dopplerClient,
                                           FirehoseProperties firehoseProperties,
                                           ProxyForwarder proxyForwarder) {
    this.dopplerClient = dopplerClient;
    this.firehoseProperties = firehoseProperties;
    this.proxyForwarder = proxyForwarder;
  }

  public void connect() {
    this.dopplerClient.firehose(
            FirehoseRequest.builder().subscriptionId(firehoseProperties.getSubscriptionId()).build())
            .subscribeOn(Schedulers.newParallel(WAVEFRONT_FIREHOSE_NOZZLE, firehoseProperties.getParallelism()))
            .filter(envelope -> filterEventType(envelope.getEventType()))
            .subscribe(envelope -> proxyForwarder.forward(envelope));
  }

  private boolean filterEventType(EventType eventType) {
    if (firehoseProperties.getEventTypes() == null || firehoseProperties.getEventTypes().size() == 0) {
      return false;
    }

    return firehoseProperties.getEventTypes().contains(eventType);
  }
}
