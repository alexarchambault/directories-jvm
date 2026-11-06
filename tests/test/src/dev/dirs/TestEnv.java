package dev.dirs;

import dev.dirs.impl.Windows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/** Helpers to build fake {@link Env} and {@link Windows} instances in tests. */
final class TestEnv {

  private TestEnv() {}

  static final String LINUX_HOME   = "/home/alice";
  static final String MAC_HOME     = "/Users/Alice";
  static final String WINDOWS_HOME = "C:\\Users\\Alice";

  static Env linux(String... envVars) {
    return env(Constants.LIN, LINUX_HOME, TestEnv::xdgUserDirs, envVars);
  }

  static Env mac(String... envVars) {
    return env(Constants.MAC, MAC_HOME, TestEnv::noXdgUserDirs, envVars);
  }

  /** On Windows, the home directory comes from the {@link Windows} supplier, not from {@code user.home}. */
  static Env windows(String... envVars) {
    return env(Constants.WIN, "C:\\Users\\not-used", TestEnv::noXdgUserDirs, envVars);
  }

  static Env unknown() {
    return env(Constants.UNKNOWN, "/home/alice", TestEnv::noXdgUserDirs);
  }

  /** @param envVars alternating environment variable names and values */
  static Env env(char os, String home, Function<String[], String[]> xdgUserDirs, String... envVars) {
    if (envVars.length % 2 != 0)
      throw new IllegalArgumentException("Expected an even number of arguments");
    Map<String, String> envMap = new HashMap<>();
    for (int i = 0; i < envVars.length; i += 2)
      envMap.put(envVars[i], envVars[i + 1]);
    Map<String, String> props = new HashMap<>();
    props.put("user.home", home);
    return new Env(os, envMap::get, props::get, xdgUserDirs);
  }

  /** Fake {@code xdg-user-dir}: returns {@code /home/alice/xdg-<NAME>} for each directory name. */
  static String[] xdgUserDirs(String... dirs) {
    return Arrays.stream(dirs).map(d -> LINUX_HOME + "/xdg-" + d).toArray(String[]::new);
  }

  static String[] noXdgUserDirs(String... dirs) {
    throw new AssertionError("xdg-user-dir shouldn't be called, got " + Arrays.toString(dirs));
  }

  private static final Map<String, String> WINDOWS_FOLDERS = new HashMap<>();
  static {
    WINDOWS_FOLDERS.put("5E6C858F-0E22-4760-9AFE-EA3317B67173", WINDOWS_HOME);                               // Profile
    WINDOWS_FOLDERS.put("3EB685DB-65F9-4CF6-A03A-E3EF65729F3D", WINDOWS_HOME + "\\AppData\\Roaming");        // RoamingAppData
    WINDOWS_FOLDERS.put("F1B32785-6FBA-4FCF-9D55-7B8E7F157091", WINDOWS_HOME + "\\AppData\\Local");          // LocalAppData
    WINDOWS_FOLDERS.put("4BD8D571-6D19-48D3-BE97-422220080E43", WINDOWS_HOME + "\\Music");                   // Music
    WINDOWS_FOLDERS.put("B4BFCC3A-DB2C-424C-B029-7FE99A87C641", WINDOWS_HOME + "\\Desktop");                 // Desktop
    WINDOWS_FOLDERS.put("FDD39AD0-238F-46AF-ADB4-6C85480369C7", WINDOWS_HOME + "\\Documents");               // Documents
    WINDOWS_FOLDERS.put("374DE290-123F-4565-9164-39C4925E467B", WINDOWS_HOME + "\\Downloads");               // Downloads
    WINDOWS_FOLDERS.put("33E28130-4E1E-4676-835A-98395C3BC3BB", WINDOWS_HOME + "\\Pictures");                // Pictures
    WINDOWS_FOLDERS.put("DFDF76A2-C82A-4D63-906A-5644AC457385", "C:\\Users\\Public");                       // Public
    WINDOWS_FOLDERS.put("A63293E8-664E-48DB-A079-DF759E0509F7", WINDOWS_HOME + "\\AppData\\Roaming\\Microsoft\\Windows\\Templates"); // Templates
    WINDOWS_FOLDERS.put("18989B1D-99B5-455B-841C-AB7C74E4DDFC", WINDOWS_HOME + "\\Videos");                  // Videos
  }

  /** A {@link Windows} supplier returning fixed paths for the known folder IDs. */
  static Supplier<Windows> fakeWindows() {
    return () -> folderIds -> Arrays.stream(folderIds)
        .map(id -> {
          String path = WINDOWS_FOLDERS.get(id);
          if (path == null)
            throw new AssertionError("Unexpected folder ID " + id);
          return path;
        })
        .toArray(String[]::new);
  }

  /** A {@link Windows} supplier that fails if used, for non-Windows platforms. */
  static Supplier<Windows> noWindows() {
    return () -> {
      throw new AssertionError("Windows supplier shouldn't be used on this platform");
    };
  }
}
