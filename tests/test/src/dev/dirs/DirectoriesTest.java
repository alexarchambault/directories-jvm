package dev.dirs;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Smoke tests, computing directories for the current system. */
public final class DirectoriesTest {

  @Test
  public void testBaseDirectories() {
    BaseDirectories baseDirs = BaseDirectories.get();
    System.out.println(baseDirs);
    assertNotNull(baseDirs.homeDir);
    assertNotNull(baseDirs.cacheDir);
    assertNotNull(baseDirs.configDir);
    assertNotNull(baseDirs.dataDir);
    assertNotNull(baseDirs.dataLocalDir);
    assertNotNull(baseDirs.preferenceDir);
    if (Constants.operatingSystem != Constants.WIN)
      assertEquals(System.getProperty("user.home"), baseDirs.homeDir);
    assertEquals(baseDirs, BaseDirectories.get());
  }

  @Test
  public void testUserDirectories() {
    UserDirectories userDirs = UserDirectories.get();
    System.out.println(userDirs);
    assertNotNull(userDirs.homeDir);
    assertEquals(BaseDirectories.get().homeDir, userDirs.homeDir);
  }

  @Test
  public void testProjectDirectories() {
    ProjectDirectories projDirs = ProjectDirectories.from("org" /*qualifier*/, "Baz Corp" /*organization*/, "Foo Bar-App" /*project*/);
    System.out.println(projDirs);
    assertNotNull(projDirs.cacheDir);
    assertNotNull(projDirs.configDir);
    assertNotNull(projDirs.dataDir);
    assertNotNull(projDirs.dataLocalDir);
    assertNotNull(projDirs.preferenceDir);
    assertTrue(projDirs.configDir, projDirs.configDir.contains(projDirs.projectPath));
    assertEquals(projDirs, ProjectDirectories.fromPath(projDirs.projectPath));
  }

  @Test
  public void testProjectDirectoriesMatchesEnv() {
    // the public factories should give the same results as the package-private ones used in the other tests
    ProjectDirectories projDirs = ProjectDirectories.from("org", "Baz Corp", "Foo Bar-App");
    if (Constants.operatingSystem != Constants.WIN)
      assertEquals(projDirs, ProjectDirectories.from("org", "Baz Corp", "Foo Bar-App", dev.dirs.impl.Windows.getDefaultSupplier(), Env.SYSTEM));
  }
}
