package dev.dirs;

import org.junit.Test;

import static dev.dirs.TestEnv.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public final class ProjectDirectoriesTest {

  @Test
  public void linuxFromPathDefaults() {
    ProjectDirectories dirs = ProjectDirectories.fromPath("barapp", noWindows(), linux());
    assertEquals("barapp",                          dirs.projectPath);
    assertEquals("/home/alice/.cache/barapp",       dirs.cacheDir);
    assertEquals("/home/alice/.config/barapp",      dirs.configDir);
    assertEquals("/home/alice/.local/share/barapp", dirs.dataDir);
    assertEquals("/home/alice/.local/share/barapp", dirs.dataLocalDir);
    assertEquals("/home/alice/.config/barapp",      dirs.preferenceDir);
    assertNull(dirs.runtimeDir);
  }

  @Test
  public void linuxFromPathXdgVariables() {
    ProjectDirectories dirs = ProjectDirectories.fromPath("barapp", noWindows(), linux(
        "XDG_CACHE_HOME",  "/xdg/cache",
        "XDG_CONFIG_HOME", "/xdg/config/",
        "XDG_DATA_HOME",   "/xdg/data",
        "XDG_RUNTIME_DIR", "/run/user/1001"));
    assertEquals("/xdg/cache/barapp",      dirs.cacheDir);
    assertEquals("/xdg/config/barapp",     dirs.configDir);
    assertEquals("/xdg/data/barapp",       dirs.dataDir);
    assertEquals("/xdg/data/barapp",       dirs.dataLocalDir);
    assertEquals("/xdg/config/barapp",     dirs.preferenceDir);
    assertEquals("/run/user/1001/barapp",  dirs.runtimeDir);
  }

  @Test
  public void linuxFromPathEmptyXdgVariablesAreIgnored() {
    ProjectDirectories dirs = ProjectDirectories.fromPath("barapp", noWindows(), linux(
        "XDG_CACHE_HOME",  "",
        "XDG_CONFIG_HOME", "",
        "XDG_DATA_HOME",   "",
        "XDG_RUNTIME_DIR", ""));
    assertEquals(ProjectDirectories.fromPath("barapp", noWindows(), linux()), dirs);
  }

  @Test
  public void linuxFrom() {
    Env env = linux("XDG_RUNTIME_DIR", "/run/user/1001");
    ProjectDirectories dirs = ProjectDirectories.from("com", "Foo Corp", "Bar App", noWindows(), env);
    assertEquals("barapp", dirs.projectPath);
    assertEquals(ProjectDirectories.fromPath("barapp", noWindows(), env), dirs);
    assertEquals("/home/alice/.config/barapp",  dirs.configDir);
    assertEquals("/run/user/1001/barapp",       dirs.runtimeDir);
  }

  @Test
  public void macFromPath() {
    ProjectDirectories dirs = ProjectDirectories.fromPath("com.Foo-Corp.Bar-App", noWindows(), TestEnv.mac("XDG_RUNTIME_DIR", "/run/user/1001"));
    assertEquals("com.Foo-Corp.Bar-App",                                       dirs.projectPath);
    assertEquals("/Users/Alice/Library/Caches/com.Foo-Corp.Bar-App",               dirs.cacheDir);
    assertEquals("/Users/Alice/Library/Application Support/com.Foo-Corp.Bar-App",  dirs.configDir);
    assertEquals("/Users/Alice/Library/Application Support/com.Foo-Corp.Bar-App",  dirs.dataDir);
    assertEquals("/Users/Alice/Library/Application Support/com.Foo-Corp.Bar-App",  dirs.dataLocalDir);
    assertEquals("/Users/Alice/Library/Preferences/com.Foo-Corp.Bar-App",          dirs.preferenceDir);
    assertNull(dirs.runtimeDir);
  }

  @Test
  public void macFrom() {
    ProjectDirectories dirs = ProjectDirectories.from("com", "Foo Corp", "Bar App", noWindows(), TestEnv.mac());
    assertEquals("com.Foo-Corp.Bar-App", dirs.projectPath);
    assertEquals(ProjectDirectories.fromPath("com.Foo-Corp.Bar-App", noWindows(), TestEnv.mac()), dirs);
  }

  @Test
  public void windowsFromPath() {
    ProjectDirectories dirs = ProjectDirectories.fromPath("Foo Corp\\Bar App", fakeWindows(), TestEnv.windows("XDG_RUNTIME_DIR", "/run/user/1001"));
    assertEquals("Foo Corp\\Bar App",                                          dirs.projectPath);
    assertEquals("C:\\Users\\Alice\\AppData\\Local\\Foo Corp\\Bar App\\cache",    dirs.cacheDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Roaming\\Foo Corp\\Bar App\\config", dirs.configDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Roaming\\Foo Corp\\Bar App\\data",   dirs.dataDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Local\\Foo Corp\\Bar App\\data",     dirs.dataLocalDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Roaming\\Foo Corp\\Bar App\\config", dirs.preferenceDir);
    assertNull(dirs.runtimeDir);
  }

  @Test
  public void windowsFrom() {
    ProjectDirectories dirs = ProjectDirectories.from("com", "Foo Corp", "Bar App", fakeWindows(), TestEnv.windows());
    assertEquals("Foo Corp\\Bar App", dirs.projectPath);
    assertEquals(ProjectDirectories.fromPath("Foo Corp\\Bar App", fakeWindows(), TestEnv.windows()), dirs);
  }

  @Test
  public void linuxFromNullApplicationFallsBackToOrganization() {
    ProjectDirectories dirs = ProjectDirectories.from("com", "Foo Corp", null, noWindows(), linux());
    assertEquals("foocorp", dirs.projectPath);
    assertEquals(ProjectDirectories.fromPath("foocorp", noWindows(), linux()), dirs);
  }

  @Test
  public void linuxFromEmptyApplicationFallsBackToOrganization() {
    assertEquals("foocorp", ProjectDirectories.from("com", "Foo Corp", "", noWindows(), linux()).projectPath);
  }

  @Test
  public void linuxFromBlankApplicationFallsBackToOrganization() {
    assertEquals("foocorp", ProjectDirectories.from("com", "Foo Corp", "   ", noWindows(), linux()).projectPath);
  }

  @Test
  public void fromRequiresOrganizationOrApplication() {
    assertThrows(UnsupportedOperationException.class, () -> ProjectDirectories.from("com", "", "", noWindows(), linux()));
    assertThrows(UnsupportedOperationException.class, () -> ProjectDirectories.from("com", null, null, noWindows(), linux()));
    assertThrows(UnsupportedOperationException.class, () -> ProjectDirectories.from("com", "  ", " ", noWindows(), linux()));
  }

  @Test
  public void unknownOperatingSystem() {
    assertThrows(UnsupportedOperatingSystemException.class, () -> ProjectDirectories.fromPath("barapp", noWindows(), unknown()));
    assertThrows(UnsupportedOperatingSystemException.class, () -> ProjectDirectories.from("com", "Foo Corp", "Bar App", noWindows(), unknown()));
  }

  @Test
  public void equalsAndHashCode() {
    ProjectDirectories a = ProjectDirectories.fromPath("barapp", noWindows(), linux("XDG_RUNTIME_DIR", "/run/user/1001"));
    ProjectDirectories b = ProjectDirectories.fromPath("barapp", noWindows(), linux("XDG_RUNTIME_DIR", "/run/user/1001"));
    ProjectDirectories c = ProjectDirectories.fromPath("barapp", noWindows(), linux());
    ProjectDirectories d = ProjectDirectories.fromPath("bazapp", noWindows(), linux());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c); // differ by runtimeDir only
    assertNotEquals(c, a);
    assertNotEquals(c, d);
    assertNotEquals(c, null);
    assertNotEquals(c, "foo");
  }
}
