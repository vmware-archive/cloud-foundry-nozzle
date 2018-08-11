package com.wavefront;

import com.wavefront.model.AppEnvelope;
import com.wavefront.model.AppInfo;

import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.dropsonde.events.CounterEvent;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for equality and hashCode for AppEnvelope
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class AppEnvelopeTest {
  @Test
  public void testEquality() {
    long timestamp = System.currentTimeMillis();
    assertTrue(constructEnvelope(timestamp).equals(constructEnvelope(timestamp)));
    // 1) Both Optional AppInfo
    assertTrue(new AppEnvelope(constructEnvelope(timestamp), Optional.empty()).equals(
        new AppEnvelope(constructEnvelope(timestamp), Optional.empty())));
    assertTrue(new AppEnvelope(constructEnvelope(timestamp), Optional.empty()).
        hashCode() == new AppEnvelope(constructEnvelope(timestamp), Optional.empty()).hashCode());

    // 2) Both non-optional AppInfo
    assertTrue(new AppEnvelope(constructEnvelope(timestamp), Optional.of(
        new AppInfo("appName", "org", "space"))).equals(
            new AppEnvelope(constructEnvelope(timestamp), Optional.of(new AppInfo("appName", "org", "space")))));
    assertTrue(new AppEnvelope(constructEnvelope(timestamp), Optional.of(
        new AppInfo("appName", "org", "space"))).
        hashCode() == new AppEnvelope(constructEnvelope(timestamp),
        Optional.of(new AppInfo("appName", "org", "space"))).hashCode());
  }

  @Test
  public void testUnequality() {
    long timestamp = System.currentTimeMillis();

    // 1) One optional
    assertFalse(new AppEnvelope(constructEnvelope(timestamp), Optional.empty()).
        hashCode() == new AppEnvelope(constructEnvelope(timestamp), Optional.of(
            new AppInfo("appName", "org", "space"))).hashCode());
    assertFalse(new AppEnvelope(constructEnvelope(timestamp), Optional.empty()).equals(
        new AppEnvelope(constructEnvelope(timestamp), Optional.of(new AppInfo("appName", "org", "space")))));

    // 3) not equal Envelope
    assertFalse(new AppEnvelope(constructEnvelope(timestamp),
        Optional.of(new AppInfo("appName", "org", "space"))).
        hashCode() == new AppEnvelope(constructEnvelope(timestamp + 1),
            Optional.of(new AppInfo("appName", "org", "space"))).hashCode());
    assertFalse(new AppEnvelope(constructEnvelope(timestamp),
        Optional.of(new AppInfo("appName", "org", "space"))).equals(
        new AppEnvelope(constructEnvelope(timestamp + 1),
            Optional.of(new AppInfo("appName", "org", "space")))));

    // 3) not equal AppInfo
    assertFalse(new AppEnvelope(constructEnvelope(timestamp), Optional.of(
        new AppInfo("appName1", "org1", "space1"))).
        hashCode() == new AppEnvelope(constructEnvelope(timestamp),
            Optional.of(new AppInfo("appName2", "org2", "space2"))).hashCode());
    assertFalse(new AppEnvelope(constructEnvelope(timestamp), Optional.of(
        new AppInfo("appName1", "org1", "space1"))).equals(
        new AppEnvelope(constructEnvelope(timestamp),
            Optional.of(new AppInfo("appName2", "org2", "space2")))));
  }

  private Envelope constructEnvelope(long timestampMillis) {
    CounterEvent counterEvent = new CounterEvent.Builder().name("eventName").total(1000L).
        delta(1L).build();
    org.cloudfoundry.dropsonde.events.Envelope dropSondeEnvelope =
        new org.cloudfoundry.dropsonde.events.Envelope.Builder().index("blah").
            eventType(org.cloudfoundry.dropsonde.events.Envelope.EventType.CounterEvent).
            origin("blah").deployment("blah").job("blah").ip("blah").
            timestamp(timestampMillis).counterEvent(counterEvent).build();
    return Envelope.from(dropSondeEnvelope);
  }
}
