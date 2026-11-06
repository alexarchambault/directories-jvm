package dev.dirs.impl;

import dev.dirs.Constants;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public final class LinuxTest {

  @Test
  public void runtimeDir() {
    assertEquals("/run/user/1000/barapp", Linux.runtimeDir("/run/user/1000", "barapp"));
    assertEquals("/run/user/1000/barapp", Linux.runtimeDir("/run/user/1000/", "barapp"));
    assertEquals("/run/user/1000/barapp", Linux.runtimeDir("/run/user/1000/", "/barapp"));
    assertEquals("/run/user/1000", Linux.runtimeDir("/run/user/1000", null));
    assertNull(Linux.runtimeDir(null, "barapp"));
    assertNull(Linux.runtimeDir("", "barapp"));
  }

  private static void assumeUnix() {
    assumeTrue("Unix-only test", Constants.operatingSystem != 'w' && new File("/bin/sh").exists());
  }

  private static String fakeXdgUserDir(String body) throws IOException {
    Path script = Files.createTempFile("fake-xdg-user-dir", ".sh");
    script.toFile().deleteOnExit();
    Files.write(script, ("#!/bin/sh\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
    Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
    return script.toString();
  }

  @Test
  public void xdgUserDirs() throws IOException {
    assumeUnix();
    String cmd = fakeXdgUserDir("echo \"/home/alice/$1 dir\"");
    String[] dirs = Linux.getXDGUserDirs(cmd, 10000L, "MUSIC", "DESKTOP", "A;B $(false)");
    assertArrayEquals(new String[] {"/home/alice/MUSIC dir", "/home/alice/DESKTOP dir", "/home/alice/A;B $(false) dir"}, dirs);
  }

  @Test
  public void xdgUserDirsMissingCommand() {
    assumeUnix();
    String[] dirs = Linux.getXDGUserDirs("/nonexistent/xdg-user-dir", 10000L, "MUSIC", "DESKTOP");
    assertArrayEquals(new String[] {null, null}, dirs);
  }

  @Test
  public void xdgUserDirsPartialFailure() throws IOException {
    assumeUnix();
    String cmd = fakeXdgUserDir("if [ \"$1\" = DESKTOP ]; then echo oops >&2; exit 1; fi; echo \"/home/alice/$1\"");
    String[] dirs = Linux.getXDGUserDirs(cmd, 10000L, "MUSIC", "DESKTOP", "VIDEOS");
    assertArrayEquals(new String[] {"/home/alice/MUSIC", null, "/home/alice/VIDEOS"}, dirs);
  }

  @Test
  public void xdgUserDirsLotsOfStderr() throws IOException {
    assumeUnix();
    // way more than a pipe buffer, would block the process if stderr wasn't drained or discarded
    String cmd = fakeXdgUserDir("i=0; while [ $i -lt 5000 ]; do echo 'some noise on stderr, some noise on stderr' >&2; i=$((i+1)); done; echo \"/home/alice/$1\"");
    String[] dirs = Linux.getXDGUserDirs(cmd, 60000L, "MUSIC");
    assertArrayEquals(new String[] {"/home/alice/MUSIC"}, dirs);
  }

  @Test
  public void xdgUserDirsTimeout() throws IOException {
    assumeUnix();
    String cmd = fakeXdgUserDir("sleep 10; echo \"/home/alice/$1\"");
    long start = System.nanoTime();
    String[] dirs = Linux.getXDGUserDirs(cmd, 300L, "MUSIC", "DESKTOP");
    long elapsedMillis = (System.nanoTime() - start) / 1000000L;
    assertArrayEquals(new String[] {null, null}, dirs);
    assertTrue("took " + elapsedMillis + " ms", elapsedMillis < 8000L);
  }

  @Test
  public void xdgUserDirsNonAscii() throws IOException {
    assumeUnix();
    assumeTrue("Needs UTF-8 file path encoding", "UTF-8".equalsIgnoreCase(System.getProperty("sun.jnu.encoding")));
    // raw UTF-8 bytes of "é" and "😀"
    String cmd = fakeXdgUserDir("printf '/home/alice/\\303\\251\\360\\237\\230\\200\\n'");
    String[] dirs = Linux.getXDGUserDirs(cmd, 10000L, "MUSIC");
    assertArrayEquals(new String[] {"/home/alice/é😀"}, dirs);
  }

  @Test
  public void xdgUserDirsDefaultCommand() {
    assumeUnix();
    // must not throw, whether xdg-user-dir is installed or not
    String[] dirs = Linux.getXDGUserDirs("MUSIC", "DESKTOP");
    assertEquals(2, dirs.length);
  }
}
