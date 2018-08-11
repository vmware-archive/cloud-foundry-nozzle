package com.wavefront;

import com.codahale.metrics.MetricRegistry;
import com.wavefront.model.AppEnvelope;
import com.wavefront.props.AppInfoProperties;
import com.wavefront.props.FirehoseProperties;
import com.wavefront.proxy.ProxyForwarder;
import com.wavefront.service.AppInfoFetcher;
import com.wavefront.service.FirehoseToWavefrontProxyConnector;
import com.wavefront.service.FirehoseToWavefrontProxyConnectorImpl;
import com.wavefront.service.WavefrontMetricsReporter;

import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.dropsonde.events.CounterEvent;
import org.easymock.EasyMock;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import reactor.core.publisher.Flux;

/**
 * Wavefront Firehose Nozzle Unit Tests
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class WavefrontNozzleApplicationTests {

  @Test
  public void testFirehoseConnectorProxyForwarder() throws IOException, InterruptedException {
    FirehoseProperties firehoseProperties = new FirehoseProperties();
    firehoseProperties.setSubscriptionId("subscriptionId");
    firehoseProperties.setEventTypes(Arrays.asList(EventType.COUNTER_EVENT,
        EventType.VALUE_METRIC, EventType.CONTAINER_METRIC));
    AppInfoProperties appInfoProperties = new AppInfoProperties();

    testInternal(firehoseProperties, appInfoProperties, "CounterEvent",
        EventType.COUNTER_EVENT);
    testInternal(firehoseProperties, appInfoProperties, "ValueMetric",
        EventType.VALUE_METRIC);
    testInternal(firehoseProperties, appInfoProperties, "ContainerMetric",
        EventType.CONTAINER_METRIC);
    testInternal(firehoseProperties, appInfoProperties, "Error",
        EventType.ERROR);
    testInternal(firehoseProperties, appInfoProperties, "HttpStartStop",
        EventType.HTTP_START_STOP);
    testInternal(firehoseProperties, appInfoProperties, "LogMessage",
        EventType.LOG_MESSAGE);
  }

  private void testInternal(FirehoseProperties firehoseProperties,
                            AppInfoProperties appInfoProperties, String eventName,
                            EventType eventType)
      throws InterruptedException {
    Envelope envelope = dummyEnvelope(eventName, eventType);
    DopplerClient dopplerClient = EasyMock.createMock(DopplerClient.class);
    EasyMock.expect(dopplerClient.firehose(EasyMock.anyObject())).andReturn(Flux.just(envelope));
    ProxyForwarder proxyForwarder = EasyMock.createMock(ProxyForwarder.class);
    AppInfoFetcher appInfoFetcher = EasyMock.createMock(AppInfoFetcher.class);

    if (firehoseProperties.getEventTypes().stream().anyMatch(
        type -> type.toString().equals(eventType.toString()))) {
      proxyForwarder.forward(new AppEnvelope(envelope, Optional.empty()));
      EasyMock.expectLastCall().once();
    }

    EasyMock.replay(dopplerClient, proxyForwarder);

    WavefrontMetricsReporter wavefrontMetricsReporter = new WavefrontMetricsReporter();
    wavefrontMetricsReporter.setMetricRegistry(new MetricRegistry());
    FirehoseToWavefrontProxyConnector proxyConnector = new FirehoseToWavefrontProxyConnectorImpl(
        wavefrontMetricsReporter, dopplerClient, firehoseProperties, appInfoProperties,
        proxyForwarder, appInfoFetcher);
    proxyConnector.connect();
    // proxyForwarder.forward() is invoked on a different thread and
    // EasyMock.verify() is invoked on the current thread
    // TODO - not the best way to do thread synchronization ...
    Thread.sleep(2000);
    EasyMock.verify(dopplerClient, proxyForwarder);
  }

  private Envelope dummyEnvelope(String eventName, EventType eventType) {
    CounterEvent counterEvent = new CounterEvent.Builder().name(eventName).total(1000L).
        delta(1L).build();

    org.cloudfoundry.dropsonde.events.Envelope.EventType dropsondeEventType;
    switch (eventType) {
      case COUNTER_EVENT:
        dropsondeEventType = org.cloudfoundry.dropsonde.events.Envelope.EventType.CounterEvent;
        break;
      case VALUE_METRIC:
        dropsondeEventType = org.cloudfoundry.dropsonde.events.Envelope.EventType.ValueMetric;
        break;
      case CONTAINER_METRIC:
        dropsondeEventType = org.cloudfoundry.dropsonde.events.Envelope.EventType.ContainerMetric;
        break;
      case ERROR:
        dropsondeEventType = org.cloudfoundry.dropsonde.events.Envelope.EventType.Error;
        break;
      case HTTP_START_STOP:
        dropsondeEventType = org.cloudfoundry.dropsonde.events.Envelope.EventType.HttpStartStop;
        break;
      case LOG_MESSAGE:
        dropsondeEventType = org.cloudfoundry.dropsonde.events.Envelope.EventType.LogMessage;
        break;
      default:
        throw new IllegalStateException("Invalid event type: " + eventType);
    }
    org.cloudfoundry.dropsonde.events.Envelope dropSondeEnvelope =
        new org.cloudfoundry.dropsonde.events.Envelope.Builder().index("blah").
            eventType(dropsondeEventType).origin("blah").deployment("blah").job("blah").ip("blah").
            timestamp(System.currentTimeMillis()).counterEvent(counterEvent).build();
    return Envelope.from(dropSondeEnvelope);
  }
}
