package com.wavefront.service;

import com.wavefront.model.AppEnvelope;
import com.wavefront.props.AppInfoProperties;
import com.wavefront.props.FirehoseProperties;
import com.wavefront.proxy.ProxyForwarder;

import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.FirehoseRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static com.wavefront.utils.Constants.WAVEFRONT_FIREHOSE_NOZZLE;

/**
 * FirehoseToWavefrontProxyConnector implementation
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
@Component
public class FirehoseToWavefrontProxyConnectorImpl implements FirehoseToWavefrontProxyConnector {

  private static final Logger logger = Logger.getLogger(
      FirehoseToWavefrontProxyConnectorImpl.class.getCanonicalName());

  @Autowired
  private DopplerClient dopplerClient;

  @Autowired
  private FirehoseProperties firehoseProperties;

  @Autowired
  private AppInfoProperties appInfoProperties;

  @Autowired
  private ProxyForwarder proxyForwarder;

  @Autowired
  private AppInfoFetcher appInfoFetcher;

  public FirehoseToWavefrontProxyConnectorImpl(DopplerClient dopplerClient,
                                               FirehoseProperties firehoseProperties,
                                               AppInfoProperties appInfoProperties,
                                               ProxyForwarder proxyForwarder,
                                               AppInfoFetcher appInfoFetcher) {
    this.dopplerClient = dopplerClient;
    this.firehoseProperties = firehoseProperties;
    this.appInfoProperties = appInfoProperties;
    this.proxyForwarder = proxyForwarder;
    this.appInfoFetcher = appInfoFetcher;
  }

  @Override
  public void connect() {
    logger.info(String.format("Connecting to firehose using subscription id: %s and " +
            "forwarding following event types: %s in parallel: %s",
        firehoseProperties.getSubscriptionId(),
        String.join(", ", firehoseProperties.getEventTypes().toString()),
        firehoseProperties.getParallelism()));

    dopplerClient.firehose(FirehoseRequest.builder().subscriptionId(
        firehoseProperties.getSubscriptionId()).build()).
        subscribeOn(Schedulers.newParallel(WAVEFRONT_FIREHOSE_NOZZLE,
            firehoseProperties.getParallelism())).
        filter(envelope -> filterEventType(envelope.getEventType())).
        flatMap(envelope -> {
          if (appInfoProperties.isFetchAppInfo() &&
              envelope.getEventType() == EventType.CONTAINER_METRIC) {
            return appInfoFetcher.fetch(envelope.getContainerMetric().getApplicationId()).
                map(optionalAppInfo -> new AppEnvelope(envelope, optionalAppInfo));
          } else {
            return Mono.just(new AppEnvelope(envelope, Optional.empty()));
          }
        }).subscribe(appEnvelope -> proxyForwarder.forward(appEnvelope));
    // TODO: Can apply back-pressure if nozzle cannot keep up with the firehose
    // i.e. onBackpressureBuffer(<BUFFER_SIZE>, BufferOverflowStrategy.DROP_LATEST)
    // need to figure out <BUFFER_SIZE> before we enable this ...
  }

  private boolean filterEventType(@Nullable EventType eventType) {
    if (firehoseProperties.getEventTypes() == null ||
        firehoseProperties.getEventTypes().size() == 0) {
      return false;
    }

    return firehoseProperties.getEventTypes().contains(eventType);
  }
}
