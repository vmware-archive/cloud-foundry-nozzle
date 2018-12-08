package com.wavefront.service;

import com.codahale.metrics.Counter;
import com.wavefront.model.AppEnvelope;
import com.wavefront.props.AppInfoProperties;
import com.wavefront.props.FirehoseProperties;
import com.wavefront.proxy.ProxyForwarder;

import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.FirehoseRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.logging.Level;
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

  private final DopplerClient dopplerClient;
  private final FirehoseProperties firehoseProperties;
  private final AppInfoProperties appInfoProperties;
  private final ProxyForwarder proxyForwarder;
  private final AppInfoFetcher appInfoFetcher;
  private final Counter numProcessedEvents;
  private final Counter numUnprocessedEvents;
  private final Counter numFetchAppInfoEnvelope;
  private final Counter numEmptyAppInfoEnvelope;
  private final Counter numSubscribeAppInfoEnvelope;

  public FirehoseToWavefrontProxyConnectorImpl(MetricsReporter metricsReporter,
                                               DopplerClient dopplerClient,
                                               FirehoseProperties firehoseProperties,
                                               AppInfoProperties appInfoProperties,
                                               ProxyForwarder proxyForwarder,
                                               AppInfoFetcher appInfoFetcher) {
    this.dopplerClient = dopplerClient;
    this.firehoseProperties = firehoseProperties;
    this.appInfoProperties = appInfoProperties;
    this.proxyForwarder = proxyForwarder;
    this.appInfoFetcher = appInfoFetcher;
    numProcessedEvents = metricsReporter.registerCounter("processed-events");
    numUnprocessedEvents = metricsReporter.registerCounter("unprocessed-events");
    numFetchAppInfoEnvelope = metricsReporter.registerCounter("fetch-app-info-envelope");
    numEmptyAppInfoEnvelope = metricsReporter.registerCounter("empty-app-info-envelope");
    numSubscribeAppInfoEnvelope = metricsReporter.registerCounter("subscribe-app-info-envelope");
  }

  @Override
  public void connect() {
    try {
      logger.info(String.format("Connecting to firehose using subscription id: %s and " +
              "forwarding following event types: %s in parallel: %s",
          firehoseProperties.getSubscriptionId(),
          String.join(", ", firehoseProperties.getEventTypes().toString()),
          firehoseProperties.getParallelism()));

      dopplerClient.firehose(FirehoseRequest.builder().subscriptionId(
          firehoseProperties.getSubscriptionId()).build()).
          subscribeOn(Schedulers.newParallel(WAVEFRONT_FIREHOSE_NOZZLE,
              firehoseProperties.getParallelism())).
          filter(envelope -> {
            boolean ret = filterEventType(envelope.getEventType());
            if (ret) {
              numProcessedEvents.inc();
            } else {
              numUnprocessedEvents.inc();
            }
            return ret;
          }).flatMap(envelope -> {
        if (appInfoProperties.isFetchAppInfo() &&
            envelope.getEventType() == EventType.CONTAINER_METRIC) {
          numFetchAppInfoEnvelope.inc();
          return appInfoFetcher.fetch(envelope.getContainerMetric().getApplicationId()).
              map(optionalAppInfo -> new AppEnvelope(envelope, optionalAppInfo));
        } else {
          numEmptyAppInfoEnvelope.inc();
          return Mono.just(new AppEnvelope(envelope, Optional.empty()));
        }
      }).subscribe(appEnvelope -> {
        numSubscribeAppInfoEnvelope.inc();
        proxyForwarder.forward(appEnvelope);
      });
      // TODO: Can apply back-pressure if nozzle cannot keep up with the firehose
      // i.e. onBackpressureBuffer(<BUFFER_SIZE>, BufferOverflowStrategy.DROP_LATEST)
      // need to figure out <BUFFER_SIZE> before we enable this ...
    } catch (IllegalStateException e) {
      /*
       *  Need to retry, see below issues for more details
       *  1) https://github.com/cloudfoundry/cf-java-client/issues/880
       *  2) https://github.com/cloudfoundry/cf-java-client/issues/901
       *  3) https://github.com/cloudfoundry/cf-java-client/issues/904
       *
       *  I hate this ugly hack !!!
       *  will be fixed when cf-java-client will support v2, see ticket 904 above
       */
      if (e.getMessage().startsWith("Required field not set:")) {
        connect();
      } else {
        logger.log(Level.SEVERE, "Wavefront firehose nozzle failed.", e);
        throw e;
      }
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Wavefront firehose nozzle failed.", t);
      throw t;
    }
  }

  private boolean filterEventType(@Nullable EventType eventType) {
    if (firehoseProperties.getEventTypes() == null ||
        firehoseProperties.getEventTypes().size() == 0) {
      return false;
    }
    return firehoseProperties.getEventTypes().contains(eventType);
  }
}
