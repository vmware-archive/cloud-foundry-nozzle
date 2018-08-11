package com.wavefront;

import com.wavefront.model.AppInfo;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for equality and hashCode for AppInfo
 *
 * @author Sushant Dewan (sushant@wavefront.com).
 */
public class AppInfoTest {

  @Test
  public void testEquality() {
    AppInfo first = new AppInfo("app", "org", "space");
    AppInfo second = new AppInfo("app", "org", "space");
    assertTrue(first.equals(second));
    assertTrue(first.hashCode() == second.hashCode());
  }

  @Test
  public void testUnequality() {
    AppInfo first = new AppInfo("app1", "org", "space");
    AppInfo second = new AppInfo("app2", "org", "space");
    assertFalse(first.hashCode() == second.hashCode());
    // If hashCode of 2 objects is different then they can never be equal
    assertFalse(first.equals(second));
  }
}
