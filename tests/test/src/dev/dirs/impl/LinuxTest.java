package dev.dirs.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
}
