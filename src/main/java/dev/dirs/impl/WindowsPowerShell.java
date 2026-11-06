package dev.dirs.impl;

import dev.dirs.Constants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class WindowsPowerShell implements Windows {

  public WindowsPowerShell() {}

  private static final String UTF8_BOM = "\ufeff";
  // This string needs to end up being a multiple of 3 bytes after conversion to UTF-16. (It is currently 1200 bytes.)
  // This is because Base64 converts 3 bytes to 4 letters; other numbers of bytes would introduce padding, which
  // would make it harder to simply concatenate this precomputed string with whatever directories the user requests.
  static final String SCRIPT_START_BASE64 = Constants.operatingSystem == 'w' ? toUTF16LEBase64("& {\n" +
      "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n" +
      "Add-Type @\"\n" +
      "using System;\n" +
      "using System.Runtime.InteropServices;\n" +
      "public class Dir {\n" +
      "  [DllImport(\"shell32.dll\")]\n" +
      "  private static extern int SHGetKnownFolderPath([MarshalAs(UnmanagedType.LPStruct)] Guid rfid, uint dwFlags, IntPtr hToken, out IntPtr pszPath);\n" +
      "  public static string GetKnownFolderPath(string rfid) {\n" +
      "    IntPtr pszPath;\n" +
      "    if (SHGetKnownFolderPath(new Guid(rfid), 0, IntPtr.Zero, out pszPath) != 0) return \"\";\n" +
      "    string path = Marshal.PtrToStringUni(pszPath);\n" +
      "    Marshal.FreeCoTaskMem(pszPath);\n" +
      "    return path;\n" +
      "  }\n" +
      "}\n" +
      "\"@\n") : null;

  /**
   * Returns the paths of the passed known folders, by running a PowerShell script.
   * <p>
   * The paths of known folders that can't be retrieved (unknown GUID, folder not available
   * for the current user, ...) are returned as empty strings.
   * </p>
   * <p>
   * If PowerShell can't be found, or if it fails (non-zero exit code, missing output lines,
   * no exit before the timeout, ...), this throws a {@link RuntimeException}, with PowerShell's
   * stderr output in its message. This never returns {@code null} elements.
   * </p>
   * <p>
   * The timeout defaults to 60 seconds, and can be changed via the
   * {@code coursier.directories.powershell-timeout} Java property (in seconds). Debug output,
   * including the PowerShell commands that are run and their stderr output, can be enabled with
   * the {@code coursier.directories.powershell-debug} Java property or the
   * {@code COURSIER_DIRECTORIES_POWERSHELL_DEBUG} environment variable, set to {@code true}.
   * </p>
   */
  public String[] winDirs(String... guids) {
    int guidsLength = guids.length;
    StringBuilder buf = new StringBuilder(guidsLength * 68);
    for (int i = 0; i < guidsLength; i++) {
      buf.append("[Dir]::GetKnownFolderPath(\"");
      buf.append(guids[i]);
      buf.append("\")\n");
    }

    String encodedCommand = SCRIPT_START_BASE64 + toUTF16LEBase64(buf + "}");

    // PowerShell executables on PATH first, then those in default locations
    Set<File> candidates = new LinkedHashSet<>();
    String path = System.getenv("Path");
    if (path != null)
      addCandidates(candidates, path.split(File.pathSeparator));
    addCandidates(candidates, fallbackDirs());

    if (candidates.isEmpty())
      throw new RuntimeException("Couldn't find pwsh.exe or powershell.exe on path or in default system locations");

    long timeoutMs = timeoutMs();
    IOException firstException = null;
    for (File candidate : candidates) {
      try {
        return runCommands(guidsLength, Charset.forName("UTF-8"), timeoutMs,
            candidate.toString(),
            "-NoProfile",
            "-NonInteractive",
            "-EncodedCommand",
            encodedCommand
        );
      } catch (IOException e) {
        if (firstException == null)
          firstException = e;
        else
          firstException.addSuppressed(e);
        // Don't make users wait for the timeout several times
        if (e instanceof ProcessTimeoutException || e instanceof InterruptedIOException)
          break;
      }
    }
    throw new RuntimeException("Failed to get Windows known folder paths via PowerShell: " + firstException.getMessage(), firstException);
  }

  static String toUTF16LEBase64(String script) {
    byte[] scriptInUtf16LEBytes = script.getBytes(Charset.forName("UTF-16LE"));
    return Base64.getEncoder().encodeToString(scriptInUtf16LEBytes);
  }

  private static boolean debug() {
    String fromEnv = System.getenv("COURSIER_DIRECTORIES_POWERSHELL_DEBUG");
    if ("true".equals(fromEnv))
      return true;
    return Boolean.getBoolean("coursier.directories.powershell-debug");
  }

  private static final long DEFAULT_TIMEOUT_MS = 60000L;

  private static long timeoutMs() {
    String value = System.getProperty("coursier.directories.powershell-timeout");
    if (value != null) {
      try {
        long seconds = Long.parseLong(value.trim());
        if (seconds > 0)
          return seconds * 1000L;
      } catch (NumberFormatException e) {
        // ignored, using the default
      }
    }
    return DEFAULT_TIMEOUT_MS;
  }

  static final class ProcessTimeoutException extends IOException {
    ProcessTimeoutException(String message) {
      super(message);
    }
  }

  /** Reads a stream fully, in a background thread */
  private static final class StreamGobbler extends Thread {
    private final InputStreamReader reader;
    private final StringBuilder output = new StringBuilder();
    private IOException exception = null;

    StreamGobbler(InputStream stream, Charset charset, String name) {
      super(name);
      setDaemon(true);
      reader = new InputStreamReader(stream, charset);
    }

    @Override
    public void run() {
      char[] buf = new char[4096];
      try {
        int read;
        while ((read = reader.read(buf)) >= 0) {
          synchronized (this) {
            output.append(buf, 0, read);
          }
        }
      } catch (IOException e) {
        synchronized (this) {
          exception = e;
        }
      } finally {
        try {
          reader.close();
        } catch (IOException e) {
          // ignored
        }
      }
    }

    synchronized String output() {
      return output.toString();
    }

    synchronized IOException exception() {
      return exception;
    }
  }

  private static String stderrSuffix(String stderr) {
    String trimmed = stderr.trim();
    return trimmed.isEmpty() ? " (no stderr output)" : ", stderr:" + System.lineSeparator() + trimmed;
  }

  private static InterruptedIOException interrupted(String message, InterruptedException cause) {
    Thread.currentThread().interrupt();
    InterruptedIOException ex = new InterruptedIOException(message);
    ex.initCause(cause);
    return ex;
  }

  /**
   * Runs a command, and returns the first {@code expectedResultLines} lines of its output.
   * <p>
   * Both stdout and stderr are read in background threads, so that the process never blocks
   * writing to them. A UTF-8 BOM at the beginning of the output is removed.
   * </p>
   *
   * @throws IOException if the command can't be started, doesn't exit within {@code timeoutMs}
   *                     ({@link ProcessTimeoutException}), exits with a non-zero exit code,
   *                     or prints fewer lines than expected
   */
  static String[] runCommands(int expectedResultLines, Charset charset, long timeoutMs, String... commands) throws IOException {
    boolean debug = debug();
    String commandName = commands[0];
    if (debug)
      System.err.println("Running command " + String.join(" ", commands));

    final Process process = new ProcessBuilder(commands).start();
    try {
      // we don't write anything to the process stdin
      process.getOutputStream().close();

      StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream(), charset, "directories-stdout-reader");
      StreamGobbler stderrGobbler = new StreamGobbler(process.getErrorStream(), charset, "directories-stderr-reader");
      stdoutGobbler.start();
      stderrGobbler.start();

      boolean exited;
      try {
        exited = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        throw interrupted("Interrupted while waiting for " + commandName, e);
      }

      if (!exited) {
        process.destroyForcibly();
        String stderr = stderrGobbler.output();
        if (debug)
          System.err.println(commandName + " timed out, stderr:" + System.lineSeparator() + stderr);
        throw new ProcessTimeoutException(commandName + " didn't exit within " + timeoutMs + " ms" + stderrSuffix(stderr));
      }

      // The streams get closed once the process exited, unless sub-processes inherited them,
      // hence the timeouts here
      try {
        stdoutGobbler.join(5000L);
        stderrGobbler.join(5000L);
      } catch (InterruptedException e) {
        throw interrupted("Interrupted while reading the output of " + commandName, e);
      }

      int exitCode = process.exitValue();
      String stdout = stdoutGobbler.output();
      String stderr = stderrGobbler.output();
      if (debug) {
        System.err.println(commandName + " exited with code " + exitCode);
        if (!stderr.isEmpty())
          System.err.println(commandName + " stderr:" + System.lineSeparator() + stderr);
      }

      if (exitCode != 0)
        throw new IOException(commandName + " exited with code " + exitCode + stderrSuffix(stderr));
      IOException stdoutException = stdoutGobbler.exception();
      if (stdoutException != null)
        throw new IOException("Error reading the output of " + commandName, stdoutException);

      String[] results = new String[expectedResultLines];
      BufferedReader reader = new BufferedReader(new StringReader(stdout));
      for (int i = 0; i < expectedResultLines; i++) {
        String line = reader.readLine();
        if (i == 0 && line != null && line.startsWith(UTF8_BOM))
          line = line.substring(UTF8_BOM.length());
        if (line == null)
          throw new IOException(commandName + " printed " + i + " line(s), expected " + expectedResultLines + stderrSuffix(stderr));
        results[i] = line;
      }
      return results;
    } finally {
      if (process.isAlive())
        process.destroyForcibly();
    }
  }

  // legacy powershell.exe seems to run faster than pwsh.exe so prefer it if available
  private static final String[] COMMANDS = { "powershell.exe", "pwsh.exe" };

  private static void addCandidates(Set<File> candidates, String[] dirs) {
    for (String dir : dirs) {
      if (dir.isEmpty())
        continue;
      for (String command : COMMANDS) {
        File commandFile = new File(dir, command);
        if (commandFile.exists())
          candidates.add(commandFile);
      }
    }
  }

  private static String[] fallbackDirs() {
    File powerShellBase = new File("C:\\Program Files\\Powershell");
    String[] powerShellDirs = powerShellBase.list();
    if (powerShellDirs == null) {
      powerShellDirs = new String[0];
    }
    String[] allPowerShellDirs = new String[powerShellDirs.length + 1];

    // legacy powershell.exe seems to run faster than pwsh.exe so prefer it if available
    String systemRoot = System.getenv("SystemRoot");
    if (systemRoot == null) {
      systemRoot = "C:\\Windows";
    }
    allPowerShellDirs[0] = systemRoot + "\\System32\\WindowsPowerShell\\v1.0\\";

    for (int i = 0; i < powerShellDirs.length; ++i) {
      allPowerShellDirs[i + 1] = new File(powerShellBase, powerShellDirs[i]).toString();
    }
    return allPowerShellDirs;
  }
}
