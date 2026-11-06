package com.tv.screenguard;
import org.junit.Test;
import static org.junit.Assert.*;
public class ChildBackgroundProtectionTest {
 @Test public void onlyVerifiedFirmwareCombinationOptsIntoVendorWrites() {
  assertTrue(ChildBackgroundProtection.supportedModel("DBB3 Pro",28));
  assertFalse(ChildBackgroundProtection.supportedModel("DBB3 Pro",29));
  assertFalse(ChildBackgroundProtection.supportedModel("DBB3 Pro",26));
  assertFalse(ChildBackgroundProtection.supportedModel("DBB3",28));
  assertFalse(ChildBackgroundProtection.supportedModel("sdk_google_atv64",28));
  assertFalse(ChildBackgroundProtection.supportedModel(null,28));
 }
}
