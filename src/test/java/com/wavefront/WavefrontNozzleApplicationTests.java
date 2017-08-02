package com.wavefront;

import com.wavefront.props.FirehoseProperties;
import com.wavefront.proxy.ProxyForwarder;
import com.wavefront.proxy.ProxyForwarderImpl;
import com.wavefront.service.FirehoseToWavefrontProxyConnector;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.dropsonde.events.CounterEvent;
import org.easymock.EasyMock;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Arrays;

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

    testInternal(firehoseProperties, "CounterEvent",
            org.cloudfoundry.dropsonde.events.Envelope.EventType.CounterEvent);
    testInternal(firehoseProperties, "ValueMetric",
            org.cloudfoundry.dropsonde.events.Envelope.EventType.ValueMetric);
    testInternal(firehoseProperties, "ContainerMetric",
            org.cloudfoundry.dropsonde.events.Envelope.EventType.ContainerMetric);
    testInternal(firehoseProperties, "Error",
            org.cloudfoundry.dropsonde.events.Envelope.EventType.Error);
    testInternal(firehoseProperties, "HttpStartStop",
            org.cloudfoundry.dropsonde.events.Envelope.EventType.HttpStartStop);
    testInternal(firehoseProperties, "LogMessage",
            org.cloudfoundry.dropsonde.events.Envelope.EventType.LogMessage);
  }

  private void testInternal(FirehoseProperties firehoseProperties, String eventName,
                            org.cloudfoundry.dropsonde.events.Envelope.EventType eventType)
          throws InterruptedException {
    Envelope envelope = dummyEnvelope(eventName, eventType);
    DopplerClient dopplerClient = EasyMock.createMock(DopplerClient.class);
    EasyMock.expect(dopplerClient.firehose(EasyMock.anyObject())).andReturn(Flux.just(envelope));
    ProxyForwarder proxyForwarder = EasyMock.createMock(ProxyForwarderImpl.class);

    if (firehoseProperties.getEventTypes().stream().anyMatch(type -> type.toString().equals(eventType.toString()))) {
      proxyForwarder.forward(envelope);
      EasyMock.expectLastCall().once();
    }

    EasyMock.replay(dopplerClient, proxyForwarder);

    FirehoseToWavefrontProxyConnector proxyConnector = new FirehoseToWavefrontProxyConnector(
            dopplerClient, firehoseProperties, proxyForwarder);
    proxyConnector.connect();
    // proxyForwarder.forward() is invoked on a different thread and
    // EasyMock.verify() is invoked on the current thread
    // TODO - not the best way to do thread synchronization ...
    Thread.sleep(2000);
    EasyMock.verify(dopplerClient, proxyForwarder);
  }

  private Envelope dummyEnvelope(String eventName, org.cloudfoundry.dropsonde.events.Envelope.EventType eventType) {
    CounterEvent counterEvent = new CounterEvent.Builder().name(eventName).total(1000L).delta(1L).build();
    org.cloudfoundry.dropsonde.events.Envelope dropSondeEnvelope =
            new org.cloudfoundry.dropsonde.events.Envelope.Builder().index("blah").eventType(eventType).
                    origin("blah").deployment("blah").job("blah").ip("blah").
                    timestamp(System.currentTimeMillis()).counterEvent(counterEvent).build();
    return Envelope.from(dropSondeEnvelope);
  }
}
