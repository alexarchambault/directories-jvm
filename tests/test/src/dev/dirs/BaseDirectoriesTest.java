package dev.dirs;

import org.junit.Test;

import static dev.dirs.TestEnv.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public final class BaseDirectoriesTest {

  @Test
  public void linuxDefaults() {
    BaseDirectories dirs = BaseDirectories.get(noWindows(), linux());
    assertEquals("/home/alice",                     dirs.homeDir);
    assertEquals("/home/alice/.cache",              dirs.cacheDir);
    assertEquals("/home/alice/.config",             dirs.configDir);
    assertEquals("/home/alice/.local/share",        dirs.dataDir);
    assertEquals("/home/alice/.local/share",        dirs.dataLocalDir);
    assertEquals("/home/alice/.local/share/../bin",  dirs.executableDir);
    assertEquals("/home/alice/.config",             dirs.preferenceDir);
    assertNull(dirs.runtimeDir);
  }

  @Test
  public void linuxXdgVariables() {
    BaseDirectories dirs = BaseDirectories.get(noWindows(), linux(
        "XDG_CACHE_HOME",  "/xdg/cache",
        "XDG_CONFIG_HOME", "/xdg/config",
        "XDG_DATA_HOME",   "/xdg/data",
        "XDG_BIN_HOME",    "/xdg/bin",
        "XDG_RUNTIME_DIR", "/run/user/1001"));
    assertEquals("/home/alice",    dirs.homeDir);
    assertEquals("/xdg/cache",     dirs.cacheDir);
    assertEquals("/xdg/config",    dirs.configDir);
    assertEquals("/xdg/data",      dirs.dataDir);
    assertEquals("/xdg/data",      dirs.dataLocalDir);
    assertEquals("/xdg/bin",       dirs.executableDir);
    assertEquals("/xdg/config",    dirs.preferenceDir);
    assertEquals("/run/user/1001", dirs.runtimeDir);
  }

  @Test
  public void linuxExecutableDirFromXdgDataHome() {
    BaseDirectories dirs = BaseDirectories.get(noWindows(), linux("XDG_DATA_HOME", "/xdg/data"));
    assertEquals("/xdg/data/../bin", dirs.executableDir);
  }

  @Test
  public void linuxEmptyXdgVariablesAreIgnored() {
    BaseDirectories dirs = BaseDirectories.get(noWindows(), linux(
        "XDG_CACHE_HOME",  "",
        "XDG_CONFIG_HOME", "",
        "XDG_DATA_HOME",   "",
        "XDG_BIN_HOME",    "",
        "XDG_RUNTIME_DIR", ""));
    assertEquals(BaseDirectories.get(noWindows(), linux()), dirs);
    assertEquals("/home/alice/.cache", dirs.cacheDir);
    assertNull(dirs.runtimeDir);
  }

  @Test
  public void otherUnixesUseXdg() {
    for (char os : new char[] {Constants.BSD, Constants.SOLARIS, Constants.IBMI, Constants.AIX}) {
      BaseDirectories dirs = BaseDirectories.get(noWindows(), env(os, "/home/alice", TestEnv::noXdgUserDirs, "XDG_CACHE_HOME", "/xdg/cache"));
      assertEquals("/xdg/cache",          dirs.cacheDir);
      assertEquals("/home/alice/.config", dirs.configDir);
    }
  }

  @Test
  public void mac() {
    BaseDirectories dirs = BaseDirectories.get(noWindows(), TestEnv.mac("XDG_CACHE_HOME", "/xdg/cache", "XDG_RUNTIME_DIR", "/run/user/1001"));
    assertEquals("/Users/Alice",                                dirs.homeDir);
    assertEquals("/Users/Alice/Library/Caches",                 dirs.cacheDir);
    assertEquals("/Users/Alice/Library/Application Support",    dirs.configDir);
    assertEquals("/Users/Alice/Library/Application Support",    dirs.dataDir);
    assertEquals("/Users/Alice/Library/Application Support",    dirs.dataLocalDir);
    assertNull(dirs.executableDir);
    assertEquals("/Users/Alice/Library/Preferences",            dirs.preferenceDir);
    assertNull(dirs.runtimeDir);
  }

  @Test
  public void noTrailingSlashes() {
    for (BaseDirectories dirs : new BaseDirectories[] {
        BaseDirectories.get(noWindows(), linux()),
        BaseDirectories.get(noWindows(), linux("XDG_DATA_HOME", "/xdg/data")),
        BaseDirectories.get(noWindows(), TestEnv.mac()),
        BaseDirectories.get(fakeWindows(), TestEnv.windows())
    })
      for (String dir : new String[] {dirs.homeDir, dirs.cacheDir, dirs.configDir, dirs.dataDir, dirs.dataLocalDir, dirs.executableDir, dirs.preferenceDir, dirs.runtimeDir})
        if (dir != null)
          assertFalse(dir, dir.endsWith("/") || dir.endsWith("\\"));
  }

  @Test
  public void windows() {
    BaseDirectories dirs = BaseDirectories.get(fakeWindows(), TestEnv.windows("XDG_CACHE_HOME", "/xdg/cache"));
    assertEquals("C:\\Users\\Alice",                  dirs.homeDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Local",   dirs.cacheDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Roaming", dirs.configDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Roaming", dirs.dataDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Local",   dirs.dataLocalDir);
    assertNull(dirs.executableDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Roaming", dirs.preferenceDir);
    assertNull(dirs.runtimeDir);
  }

  @Test
  public void unknownOperatingSystem() {
    assertThrows(UnsupportedOperatingSystemException.class, () -> BaseDirectories.get(noWindows(), unknown()));
  }

  @Test
  public void equalsAndHashCode() {
    BaseDirectories a = BaseDirectories.get(noWindows(), linux("XDG_RUNTIME_DIR", "/run/user/1001"));
    BaseDirectories b = BaseDirectories.get(noWindows(), linux("XDG_RUNTIME_DIR", "/run/user/1001"));
    BaseDirectories c = BaseDirectories.get(noWindows(), linux());
    BaseDirectories d = BaseDirectories.get(noWindows(), TestEnv.mac());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertEquals(c, c);
    assertNotEquals(a, c); // differ by runtimeDir only
    assertNotEquals(c, a);
    assertNotEquals(c, d);
    assertNotEquals(c, null);
    assertNotEquals(c, "foo");
  }
}
