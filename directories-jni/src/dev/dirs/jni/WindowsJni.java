package dev.dirs.jni;

import coursier.jniutils.WindowsKnownFolders;
import dev.dirs.impl.Windows;

import java.util.function.Supplier;

public final class WindowsJni implements Windows {

  public String[] winDirs(String... folderIds) {
    String[] dirs = new String[folderIds.length];
    for (int i = 0; i < folderIds.length; i++) {
      dirs[i] = WindowsKnownFolders.knownFolderPath("{" + folderIds[i] + "}");
    }
    return dirs;
  }

  public static Supplier<Windows> getJdkAwareSupplier() {
    int jdkVersion = javaMajorVersion(System.getProperty("java.version", "0"));
    if (jdkVersion >= 22)
      return Windows.getDefaultSupplier();
    else
      return () -> new WindowsJni();
  }

  /**
   * Extracts the major version from a {@code java.version} value.
   * <p>
   * Handles both the legacy scheme ({@code 1.8.0_402} gives 8), and the JEP 223 one
   * ({@code 9}, {@code 17.0.2}, {@code 21+35}, {@code 26-ea}, ...).
   * </p>
   *
   * @param javaVersion a {@code java.version} value
   * @return the major version, or 0 if {@code javaVersion} doesn't start with digits
   */
  static int javaMajorVersion(String javaVersion) {
    if (javaVersion == null)
      return 0;
    String version = javaVersion.trim();
    if (version.startsWith("1."))
      version = version.substring("1.".length());
    int len = 0;
    while (len < version.length() && len < 9 && Character.isDigit(version.charAt(len)))
      len++;
    if (len == 0)
      return 0;
    return Integer.parseInt(version.substring(0, len));
  }

}
