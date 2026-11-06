package dev.dirs.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final class Linux {

  private Linux() {
    throw new Error();
  }


  static String ensureSingleSlash(String arg1, String arg2) {
    boolean arg1Slash = arg1.endsWith("/");
    boolean slashArg2 = arg2.startsWith("/");
    if (arg1Slash && slashArg2) {
      StringBuilder buf = new StringBuilder(arg1.length() + arg2.length() - 1);
      buf.append(arg1, 0, arg1.length() - 1).append(arg2);
      return buf.toString();
    } else if (!arg1Slash && !slashArg2) {
      return arg1 + '/' + arg2;
    } else {
      return arg1 + arg2;
    }
  }

  public static String runtimeDir(String path) {
    return runtimeDir(System.getenv("XDG_RUNTIME_DIR"), path);
  }

  static String runtimeDir(String runDir, String path) {
    if (Util.isNullOrEmpty(runDir))
      return null;
    else if (path == null)
      return runDir;
    else
      return ensureSingleSlash(runDir, path);
  }

  public static String executableDir(String homeDir, String dataDir) {
    String binDir = System.getenv("XDG_BIN_HOME");
    if (Util.isNullOrEmpty(binDir))
      return Util.defaultIfNullOrEmptyExtended(dataDir, "/../bin/", homeDir, "/.local/bin/");
    else
      return binDir;
  }

  private static final long XDG_USER_DIR_TIMEOUT_MILLIS = 10000L;

  // Prints one line per argument (empty if xdg-user-dir fails or is missing), so that
  // output lines always match the requested directories. Directory names are passed as
  // positional parameters rather than interpolated in the script.
  private static final String XDG_USER_DIR_SCRIPT =
      "cmd=\"$1\"; shift; for d in \"$@\"; do printf '%s\\n' \"$(\"$cmd\" \"$d\")\"; done";

  private static boolean debug() {
    String fromEnv = System.getenv("COURSIER_DIRECTORIES_XDG_DEBUG");
    if ("true".equals(fromEnv))
      return true;
    return Boolean.getBoolean("coursier.directories.xdg-debug");
  }

  /** Returns the XDG user directories with the passed names (such as {@code "MUSIC"} or {@code "DESKTOP"}),
    * as reported by {@code xdg-user-dir}.
    * <p>
    * This never throws: if {@code xdg-user-dir} is missing, fails, or doesn't answer in a timely manner,
    * the corresponding elements of the returned array are {@code null}, like for directories that are not set.
    * Set the {@code COURSIER_DIRECTORIES_XDG_DEBUG} environment variable to {@code true}, or the
    * {@code coursier.directories.xdg-debug} Java property to {@code true}, to print diagnostics on stderr.
    */
  public static String[] getXDGUserDirs(String... dirs) {
    return getXDGUserDirs("xdg-user-dir", XDG_USER_DIR_TIMEOUT_MILLIS, dirs);
  }

  static String[] getXDGUserDirs(String xdgUserDirCommand, long timeoutMillis, String... dirs) {
    String[] commands = new String[dirs.length + 5];
    commands[0] = "/bin/sh";
    commands[1] = "-c";
    commands[2] = XDG_USER_DIR_SCRIPT;
    commands[3] = "sh";
    commands[4] = xdgUserDirCommand;
    System.arraycopy(dirs, 0, commands, 5, dirs.length);
    String[] results = new String[dirs.length];
    try {
      String[] lines = runCommands(dirs.length, timeoutMillis, commands);
      if (lines != null)
        for (int i = 0; i < dirs.length; i++)
          results[i] = Util.isNullOrEmpty(lines[i]) ? null : lines[i];
    } catch (IOException e) {
      if (debug()) {
        System.err.println("Error running xdg-user-dir");
        e.printStackTrace();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      if (debug())
        System.err.println("Interrupted while running xdg-user-dir");
    }
    return results;
  }

  /** The charset Java uses to decode file paths: xdg-user-dir prints the raw bytes of paths,
    * so decoding them that way gives strings that map back to the same paths.
    */
  private static Charset pathCharset() {
    String[] props = {"sun.jnu.encoding", "native.encoding"};
    for (String prop : props) {
      String name = System.getProperty(prop);
      if (name != null) {
        try {
          return Charset.forName(name);
        } catch (IllegalArgumentException e) {
          // unsupported / illegal charset name, try the next one
        }
      }
    }
    return Charset.defaultCharset();
  }

  /** Returns the first {@code expectedResultLines} lines printed by the command (missing ones are null),
    * or null if the command timed out or exited with a non-zero code.
    */
  private static String[] runCommands(int expectedResultLines, long timeoutMillis, String... commands) throws IOException, InterruptedException {
    boolean debug = debug();
    if (debug)
      System.err.println("Running command " + String.join(" ", commands));

    ProcessBuilder builder = new ProcessBuilder(commands);
    builder.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
    builder.redirectError(debug ? ProcessBuilder.Redirect.INHERIT : ProcessBuilder.Redirect.to(new File("/dev/null")));
    final Process process = builder.start();

    try {
      // The output is small enough (a few short lines) to fit in the pipe buffer,
      // so we can wait for the process to exit before reading it.
      if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
        if (debug)
          System.err.println("Command timed out after " + timeoutMillis + " ms");
        return null;
      }
      int exitCode = process.exitValue();
      if (exitCode != 0) {
        if (debug)
          System.err.println("Command exited with code " + exitCode);
        return null;
      }

      String[] results = new String[expectedResultLines];
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), pathCharset()));
      try {
        for (int i = 0; i < expectedResultLines; i++)
          results[i] = reader.readLine();
      } finally {
        reader.close();
      }
      if (debug)
        System.err.println("Command output: " + Arrays.toString(results));
      return results;
    } finally {
      process.destroyForcibly();
    }
  }

}
