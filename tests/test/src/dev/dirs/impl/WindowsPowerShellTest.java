package dev.dirs.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class WindowsPowerShellTest {

  @Test
  public void base64EncodesUtf16Le() {
    // "& {" in UTF-16LE is 26 00 20 00 7b 00
    assertEquals("JgAgAHsA", WindowsPowerShell.toUTF16LEBase64("& {"));
    assertEquals("", WindowsPowerShell.toUTF16LEBase64(""));
    // non-ASCII, and non-BMP (surrogate pair) characters
    assertEquals("6QA92ADe", WindowsPowerShell.toUTF16LEBase64("\u00e9\ud83d\ude00"));
  }

}
