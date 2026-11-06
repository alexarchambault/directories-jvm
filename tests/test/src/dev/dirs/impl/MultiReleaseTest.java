package dev.dirs.impl;

import dev.dirs.Constants;

import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Checks that the JDK 22 multi-release overlay of the directories JAR is picked on JDK 22+
 * (Windows directories computed via the foreign function API), and only there (PowerShell
 * used on older JDKs).
 */
public final class MultiReleaseTest {

  private static int javaFeatureVersion() {
    String spec = System.getProperty("java.specification.version");
    if (spec.startsWith("1."))
      spec = spec.substring(2);
    return Integer.parseInt(spec);
  }

  private static boolean expectForeign() {
    return javaFeatureVersion() >= 22;
  }

  @Test
  public void testWindowsDefaultClassVersion() {
    URL url = WindowsDefault.class.getResource("WindowsDefault.class");
    assertNotNull(url);
    String urlStr = url.toString();
    assertEquals(
      "WindowsDefault class loaded from " + urlStr,
      expectForeign(),
      urlStr.contains("META-INF/versions/22/"));
  }

  @Test
  public void testDefaultWindowsImplementation() {
    // Instantiating WindowsForeign requires Windows DLLs, so this can only be checked there
    if (Constants.operatingSystem == 'w') {
      String expected = expectForeign() ? "dev.dirs.impl.WindowsForeign" : "dev.dirs.impl.WindowsPowerShell";
      assertEquals(expected, Windows.getDefaultSupplier().get().getClass().getName());
    }
  }
}
