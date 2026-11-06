package dev.dirs.impl;

import dev.dirs.Constants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public final class UtilTest {

  static {
    System.out.println("Tests Java version: " + System.getProperty("java.version"));
  }

  @Test
  public void testStripQualification01() {
    final String input    = "org.foo.BarApp";
    final String actual   = Util.stripQualification(input);
    final String expected = "BarApp";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testStripQualification02() {
    final String input    = "BarApp";
    final String actual   = Util.stripQualification(input);
    final String expected = "BarApp";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testTrimLowercaseAndReplaceWhitespaces01() {
    final String input    = "Bar App";
    final String actual   = Util.trimLowercaseReplaceWhitespace(input, "-", true);
    final String expected = "bar-app";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testTrimLowercaseAndReplaceWhitespaces02() {
    final String input    = "BarApp Foo";
    final String actual   = Util.trimLowercaseReplaceWhitespace(input, "", true);
    final String expected = "barappfoo";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testTrimLowercaseAndReplaceWhitespaces03() {
    final String input    = " Bar App ";
    final String actual   = Util.trimLowercaseReplaceWhitespace(input, "-", true);
    final String expected = "bar-app";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testTrimLowercaseAndReplaceWhitespaces04() {
    final String input    = "  Bar  App  ";
    final String actual   = Util.trimLowercaseReplaceWhitespace(input, "-", true);
    final String expected = "bar-app";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testTrimLowercaseAndReplaceWhitespacesNonBmp01() {
    final String input    = "\uD83D\uDE00 My App";
    final String actual   = Util.trimLowercaseReplaceWhitespace(input, "-", true);
    final String expected = "\uD83D\uDE00-my-app";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testTrimLowercaseAndReplaceWhitespacesNonBmp02() {
    // U+10400 DESERET CAPITAL LETTER LONG I, lowercases to U+10428
    final String input    = " \uD801\uDC00pp  \uD83D\uDE00 ";
    final String actual   = Util.trimLowercaseReplaceWhitespace(input, "", true);
    final String expected = "\uD801\uDC28pp\uD83D\uDE00";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testTrimLowercaseAndReplaceWhitespacesNonBmp03() {
    final String input    = "\uD801\uDC00 \uD801\uDC00";
    final String actual   = Util.trimLowercaseReplaceWhitespace(input, "-", false);
    final String expected = "\uD801\uDC00-\uD801\uDC00";
    assertEquals(input, expected, actual);
  }

  @Test
  public void testMacOSApplicationPath01() {
    final String inputQual = "";
    final String inputOrga = "Foo Bar";
    final String inputProj = "";
    final String actual = MacOs.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Foo-Bar", actual);
  }

  @Test
  public void testMacOSApplicationPath02() {
    final String inputQual = "";
    final String inputOrga = "";
    final String inputProj = "Baz Qux";
    final String actual = MacOs.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Baz-Qux", actual);
  }

  @Test
  public void testMacOSApplicationPath03() {
    final String inputQual = "";
    final String inputOrga = "Foo Bar";
    final String inputProj = "Baz Qux";
    final String actual = MacOs.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Foo-Bar.Baz-Qux", actual);
  }

  @Test
  public void testMacOSApplicationPath04() {
    final String inputQual = "uk.co";
    final String inputOrga = "Foo Bar";
    final String inputProj = "";
    final String actual = MacOs.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("uk.co.Foo-Bar", actual);
  }

  @Test
  public void testMacOSApplicationPath05() {
    final String inputQual = "uk.co";
    final String inputOrga = "";
    final String inputProj = "Baz Qux";
    final String actual = MacOs.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("uk.co.Baz-Qux", actual);
  }

  @Test
  public void testMacOSApplicationPath06() {
    final String inputQual = "uk.co";
    final String inputOrga = "Foo Bar";
    final String inputProj = "Baz Qux";
    final String actual = MacOs.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("uk.co.Foo-Bar.Baz-Qux", actual);
  }

  @Test
  public void testMacOSApplicationPath07() {
    final String inputQual = " uk.co ";
    final String inputOrga = " Foo  Bar ";
    final String inputProj = "  Baz Qux  ";
    final String actual = MacOs.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("uk.co.Foo-Bar.Baz-Qux", actual);
  }

  @Test
  public void testWindowsApplicationPath01() {
    final String inputQual = "";
    final String inputOrga = "Foo Bar";
    final String inputProj = "";
    final String actual = Windows.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Foo Bar", actual);
  }

  @Test
  public void testWindowsApplicationPath02() {
    final String inputQual = "";
    final String inputOrga = "";
    final String inputProj = "Baz Qux";
    final String actual = Windows.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Baz Qux", actual);
  }

  @Test
  public void testWindowsApplicationPath03() {
    final String inputQual = "";
    final String inputOrga = "Foo Bar";
    final String inputProj = "Baz Qux";
    final String actual = Windows.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Foo Bar\\Baz Qux", actual);
  }

  @Test
  public void testWindowsApplicationPath04() {
    final String inputQual = "uk.co";
    final String inputOrga = "Foo Bar";
    final String inputProj = "";
    final String actual = Windows.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Foo Bar", actual);
  }

  @Test
  public void testWindowsApplicationPath05() {
    final String inputQual = "uk.co";
    final String inputOrga = "";
    final String inputProj = "Baz Qux";
    final String actual = Windows.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Baz Qux", actual);
  }

  @Test
  public void testWindowsApplicationPath06() {
    final String inputQual = "uk.co";
    final String inputOrga = "Foo Bar";
    final String inputProj = "Baz Qux";
    final String actual = Windows.applicationPath(inputQual, inputOrga, inputProj);
    assertEquals("Foo Bar\\Baz Qux", actual);
  }

  @Test
  public void testPowershellBase64StringIsNotPadded() {
    if (Constants.operatingSystem == 'w') {
      assertFalse(WindowsPowerShell.SCRIPT_START_BASE64.endsWith("="));
    }
  }

  @Test
  public void testPowershellOne() {
    if (Constants.operatingSystem == 'w') {
      String[] winDirs = Windows.getWinDirs("3EB685DB-65F9-4CF6-A03A-E3EF65729F3D");
      for (String winDir : winDirs) {
        assertNotNull(winDir);
      }
    }
  }

  @Test
  public void testPowershell() {
    if (Constants.operatingSystem == 'w') {
      String[] winDirs = Windows.getWinDirs("3EB685DB-65F9-4CF6-A03A-E3EF65729F3D", "F1B32785-6FBA-4FCF-9D55-7B8E7F157091");
      for (String winDir : winDirs) {
        assertNotNull(winDir);
      }
    }
  }

}
