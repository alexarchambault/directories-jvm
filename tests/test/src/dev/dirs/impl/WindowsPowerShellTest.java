package dev.dirs.impl;

import dev.dirs.Constants;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class WindowsPowerShellTest {

  @Test
  public void base64EncodesUtf16Le() {
    // "& {" in UTF-16LE is 26 00 20 00 7b 00
    assertEquals("JgAgAHsA", WindowsPowerShell.toUTF16LEBase64("& {"));
    assertEquals("", WindowsPowerShell.toUTF16LEBase64(""));
    // non-ASCII, and non-BMP (surrogate pair) characters
    assertEquals("6QA92ADe", WindowsPowerShell.toUTF16LEBase64("\u00e9\ud83d\ude00"));
  }

  // The runCommands tests below rely on sh, so they don't run on Windows

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private static String[] sh(int expectedLines, long timeoutMs, String script) throws IOException {
    return WindowsPowerShell.runCommands(expectedLines, UTF_8, timeoutMs, "sh", "-c", script);
  }

  private static IOException shFailure(int expectedLines, long timeoutMs, String script) {
    try {
      String[] res = sh(expectedLines, timeoutMs, script);
      fail("Expected an exception, got " + String.join(", ", res));
      return null;
    } catch (IOException e) {
      return e;
    }
  }

  @Test
  public void runCommandsReadsOutput() throws IOException {
    if (Constants.operatingSystem == 'w')
      return;
    // stderr output, a BOM, CRLF, an empty line, extra lines, and non-ASCII characters
    String[] res = sh(3, 10000L, "printf '\\357\\273\\277a\\r\\n\\nc\\303\\251\\nextra\\n'; echo warning >&2");
    assertArrayEquals(new String[] { "a", "", "c\u00e9" }, res);
  }

  @Test
  public void runCommandsDoesntBlockOnLargeStderr() throws IOException {
    if (Constants.operatingSystem == 'w')
      return;
    // far more than the pipe buffer size, which would block the process if stderr weren't drained
    String[] res = sh(1, 30000L, "i=0; while [ $i -lt 5000 ]; do echo 'some stderr output, some stderr output' >&2; i=$((i+1)); done; echo ok");
    assertArrayEquals(new String[] { "ok" }, res);
  }

  @Test
  public void runCommandsFailsOnNonZeroExitCode() {
    if (Constants.operatingSystem == 'w')
      return;
    IOException e = shFailure(1, 10000L, "echo foo; echo 'Add-Type failed' >&2; exit 3");
    assertTrue(e.getMessage(), e.getMessage().contains("exited with code 3"));
    assertTrue(e.getMessage(), e.getMessage().contains("Add-Type failed"));
  }

  @Test
  public void runCommandsFailsOnMissingLines() {
    if (Constants.operatingSystem == 'w')
      return;
    IOException e = shFailure(3, 10000L, "echo foo; echo 'not enough' >&2");
    assertTrue(e.getMessage(), e.getMessage().contains("printed 1 line(s), expected 3"));
    assertTrue(e.getMessage(), e.getMessage().contains("not enough"));
  }

  @Test
  public void runCommandsTimesOut() {
    if (Constants.operatingSystem == 'w')
      return;
    long start = System.nanoTime();
    IOException e = shFailure(1, 500L, "echo 'hanging' >&2; exec sleep 30");
    long durationMs = (System.nanoTime() - start) / 1000000L;
    assertTrue(e.getClass().getName(), e instanceof WindowsPowerShell.ProcessTimeoutException);
    assertTrue(e.getMessage(), e.getMessage().contains("hanging"));
    assertTrue("took " + durationMs + " ms", durationMs < 10000L);
  }

  @Test
  public void runCommandsFailsOnMissingCommand() {
    if (Constants.operatingSystem == 'w')
      return;
    try {
      WindowsPowerShell.runCommands(1, UTF_8, 10000L, "/does/not/exist");
      fail("Expected an exception");
    } catch (IOException e) {
      // expected
    }
  }

}
