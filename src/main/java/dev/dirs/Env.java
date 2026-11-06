package dev.dirs;

import dev.dirs.impl.Linux;
import dev.dirs.impl.Util;

import java.util.Objects;
import java.util.function.Function;

/** The parts of the system state directories are computed from: the operating system,
  * environment variables, system properties, and the output of {@code xdg-user-dir}.
  * <p>
  * Package-private, so that tests can inject custom values, to check the per-platform logic
  * of {@link BaseDirectories}, {@link UserDirectories} and {@link ProjectDirectories} from a single platform.
  */
final class Env {

  static final Env SYSTEM = new Env(
      Constants.operatingSystem,
      System::getenv,
      System::getProperty,
      Linux::getXDGUserDirs);

  /** One of the code constants of {@link Constants} */
  final char operatingSystem;
  private final Function<String, String> getenv;
  private final Function<String, String> getProperty;
  private final Function<String[], String[]> xdgUserDirs;

  Env(
      char operatingSystem,
      Function<String, String> getenv,
      Function<String, String> getProperty,
      Function<String[], String[]> xdgUserDirs) {
    this.operatingSystem = operatingSystem;
    this.getenv          = Objects.requireNonNull(getenv);
    this.getProperty     = Objects.requireNonNull(getProperty);
    this.xdgUserDirs     = Objects.requireNonNull(xdgUserDirs);
  }

  String getenv(String name) {
    return getenv.apply(name);
  }

  String getProperty(String name) {
    return getProperty.apply(name);
  }

  /** Returns the output of {@code xdg-user-dir} for each of the passed directory names. */
  String[] xdgUserDirs(String... dirs) {
    return xdgUserDirs.apply(dirs);
  }

  String homeDir() {
    return getProperty("user.home");
  }

  /** {@code $XDG_RUNTIME_DIR}, or {@code $XDG_RUNTIME_DIR/path} if {@code path} is non-null,
    * or {@code null} if {@code $XDG_RUNTIME_DIR} is unset or empty.
    */
  String linuxRuntimeDir(String path) {
    String runDir = getenv("XDG_RUNTIME_DIR");
    if (Util.isNullOrEmpty(runDir))
      return null;
    else if (path == null)
      return runDir;
    else
      return runDir + '/' + path;
  }

  /** {@code $XDG_BIN_HOME}, or a {@code bin} directory next to {@code dataDir} if it's unset or empty. */
  String linuxExecutableDir(String homeDir, String dataDir) {
    String binDir = getenv("XDG_BIN_HOME");
    if (Util.isNullOrEmpty(binDir))
      return Util.defaultIfNullOrEmptyExtended(dataDir, "/../bin", homeDir, "/.local/bin");
    else
      return binDir;
  }
}
