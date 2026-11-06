package dev.dirs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public final class ProjectDirectoriesTest {

  private static void assumeLinux() {
    assumeTrue("Linux-only test", Constants.operatingSystem == 'l');
  }

  private static void assertProjectPath(ProjectDirectories dirs, String expectedPath) {
    assertEquals(expectedPath, dirs.projectPath);
    assertTrue(dirs.cacheDir, dirs.cacheDir.endsWith("/" + expectedPath));
    assertTrue(dirs.configDir, dirs.configDir.endsWith("/" + expectedPath));
    assertTrue(dirs.dataDir, dirs.dataDir.endsWith("/" + expectedPath));
  }

  @Test
  public void linuxUsesApplication() {
    assumeLinux();
    assertProjectPath(ProjectDirectories.from("org", "Foo Corp", "Bar App"), "barapp");
  }

  @Test
  public void linuxNullApplicationFallsBackToOrganization() {
    assumeLinux();
    assertProjectPath(ProjectDirectories.from("org", "Foo Corp", null), "foocorp");
  }

  @Test
  public void linuxEmptyApplicationFallsBackToOrganization() {
    assumeLinux();
    assertProjectPath(ProjectDirectories.from("org", "Foo Corp", ""), "foocorp");
  }

  @Test
  public void linuxBlankApplicationFallsBackToOrganization() {
    assumeLinux();
    assertProjectPath(ProjectDirectories.from("org", "Foo Corp", "   "), "foocorp");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void nullOrganizationAndApplication() {
    ProjectDirectories.from("org", null, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void linuxBlankOrganizationAndApplication() {
    assumeLinux();
    ProjectDirectories.from("org", "  ", " ");
  }
}
