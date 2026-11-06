package dev.dirs;

import org.junit.Test;

import static dev.dirs.TestEnv.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public final class UserDirectoriesTest {

  @Test
  public void linux() {
    UserDirectories dirs = UserDirectories.get(noWindows(), TestEnv.linux());
    assertEquals("/home/alice",                   dirs.homeDir);
    assertEquals("/home/alice/xdg-MUSIC",         dirs.audioDir);
    assertEquals("/home/alice/xdg-DESKTOP",       dirs.desktopDir);
    assertEquals("/home/alice/xdg-DOCUMENTS",     dirs.documentDir);
    assertEquals("/home/alice/xdg-DOWNLOAD",      dirs.downloadDir);
    assertEquals("/home/alice/.local/share/fonts", dirs.fontDir);
    assertEquals("/home/alice/xdg-PICTURES",      dirs.pictureDir);
    assertEquals("/home/alice/xdg-PUBLICSHARE",   dirs.publicDir);
    assertEquals("/home/alice/xdg-TEMPLATES",     dirs.templateDir);
    assertEquals("/home/alice/xdg-VIDEOS",        dirs.videoDir);
  }

  @Test
  public void linuxXdgDataHome() {
    assertEquals("/xdg/data/fonts", UserDirectories.get(noWindows(), TestEnv.linux("XDG_DATA_HOME", "/xdg/data")).fontDir);
    assertEquals("/home/alice/.local/share/fonts", UserDirectories.get(noWindows(), TestEnv.linux("XDG_DATA_HOME", "")).fontDir);
  }

  @Test
  public void ibmi() {
    UserDirectories dirs = UserDirectories.get(noWindows(), env(Constants.IBMI, "/home/alice", TestEnv::noXdgUserDirs, "XDG_DATA_HOME", "/xdg/data"));
    assertEquals("/home/alice",           dirs.homeDir);
    assertEquals("/home/alice/Music",     dirs.audioDir);
    assertEquals("/home/alice/Documents", dirs.documentDir);
    assertEquals("/xdg/data/fonts",       dirs.fontDir);
    assertNull(dirs.templateDir);
  }

  @Test
  public void mac() {
    UserDirectories dirs = UserDirectories.get(noWindows(), TestEnv.mac("XDG_DATA_HOME", "/xdg/data"));
    assertEquals("/Users/Alice",               dirs.homeDir);
    assertEquals("/Users/Alice/Music",         dirs.audioDir);
    assertEquals("/Users/Alice/Desktop",       dirs.desktopDir);
    assertEquals("/Users/Alice/Documents",     dirs.documentDir);
    assertEquals("/Users/Alice/Downloads",     dirs.downloadDir);
    assertEquals("/Users/Alice/Library/Fonts", dirs.fontDir);
    assertEquals("/Users/Alice/Pictures",      dirs.pictureDir);
    assertEquals("/Users/Alice/Public",        dirs.publicDir);
    assertNull(dirs.templateDir);
    assertEquals("/Users/Alice/Movies",        dirs.videoDir);
  }

  @Test
  public void windows() {
    UserDirectories dirs = UserDirectories.get(fakeWindows(), TestEnv.windows());
    assertEquals("C:\\Users\\Alice",            dirs.homeDir);
    assertEquals("C:\\Users\\Alice\\Music",     dirs.audioDir);
    assertEquals("C:\\Users\\Alice\\Desktop",   dirs.desktopDir);
    assertEquals("C:\\Users\\Alice\\Documents", dirs.documentDir);
    assertEquals("C:\\Users\\Alice\\Downloads", dirs.downloadDir);
    assertNull(dirs.fontDir);
    assertEquals("C:\\Users\\Alice\\Pictures",  dirs.pictureDir);
    assertEquals("C:\\Users\\Public",           dirs.publicDir);
    assertEquals("C:\\Users\\Alice\\AppData\\Roaming\\Microsoft\\Windows\\Templates", dirs.templateDir);
    assertEquals("C:\\Users\\Alice\\Videos",    dirs.videoDir);
  }

  @Test
  public void unknownOperatingSystem() {
    assertThrows(UnsupportedOperatingSystemException.class, () -> UserDirectories.get(noWindows(), unknown()));
  }

  @Test
  public void equalsAndHashCode() {
    UserDirectories a = UserDirectories.get(noWindows(), TestEnv.linux("XDG_DATA_HOME", "/xdg/data"));
    UserDirectories b = UserDirectories.get(noWindows(), TestEnv.linux("XDG_DATA_HOME", "/xdg/data"));
    UserDirectories c = UserDirectories.get(noWindows(), TestEnv.linux());
    UserDirectories d = UserDirectories.get(noWindows(), TestEnv.mac());
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c); // differ by fontDir only
    assertNotEquals(c, a);
    assertNotEquals(c, d);
    assertNotEquals(c, null);
    assertNotEquals(c, "foo");
  }
}
