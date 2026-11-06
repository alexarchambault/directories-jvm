package dev.dirs.jni;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class JavaVersionTest {

  private static void check(String javaVersion, int expected) {
    assertEquals(javaVersion, expected, WindowsJni.javaMajorVersion(javaVersion));
  }

  @Test
  public void legacyVersions() {
    check("1.8.0_402", 8);
    check("1.8", 8);
    check("1.7.0", 7);
  }

  @Test
  public void jep223Versions() {
    check("9", 9);
    check("11", 11);
    check("11.0.22", 11);
    check("17.0.2", 17);
    check("21+35", 21);
    check("22", 22);
    check("26-ea", 26);
    check("23.0.1-internal", 23);
  }

  @Test
  public void invalidVersions() {
    check("", 0);
    check(null, 0);
    check("foo", 0);
    check("1.", 0);
    check("99999999999999999999", 999999999);
  }

  @Test
  public void currentVersion() {
    // shouldn't throw, and should give a sensible value
    int version = WindowsJni.javaMajorVersion(System.getProperty("java.version"));
    if (version < 8)
      throw new AssertionError("Unexpected Java version " + version + " for " + System.getProperty("java.version"));
  }
}
