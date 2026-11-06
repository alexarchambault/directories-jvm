package dev.dirs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ConstantsTest {

  @Test
  public void knownOperatingSystems() {
    assertEquals(Constants.LIN,     Constants.operatingSystemCode("Linux"));
    assertEquals(Constants.MAC,     Constants.operatingSystemCode("Mac OS X"));
    assertEquals(Constants.WIN,     Constants.operatingSystemCode("Windows 11"));
    assertEquals(Constants.BSD,     Constants.operatingSystemCode("FreeBSD"));
    assertEquals(Constants.SOLARIS, Constants.operatingSystemCode("SunOS"));
    assertEquals(Constants.IBMI,    Constants.operatingSystemCode("OS/400"));
    assertEquals(Constants.AIX,     Constants.operatingSystemCode("AIX"));
  }

  @Test
  public void unknownOperatingSystem() {
    // used to throw from the static initializer of Constants
    assertEquals(Constants.UNKNOWN, Constants.operatingSystemCode("Plan 9"));
    assertEquals(Constants.UNKNOWN, Constants.operatingSystemCode(null));
  }

  @Test
  public void currentOperatingSystem() {
    assertEquals(Constants.operatingSystemCode(System.getProperty("os.name")), Constants.operatingSystem);
  }
}
