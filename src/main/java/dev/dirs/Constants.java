package dev.dirs;

import java.util.Locale;

public class Constants {

  static final String operatingSystemName = System.getProperty("os.name");
  /** The operating system the library is running on, as one of the code constants of this class ({@link #LIN}, {@link #MAC}, {@link #WIN}, …). */
  public static final char operatingSystem;

  /** Value of {@link #operatingSystem} on Linux. */
  public static final char LIN = 'l';
  /** Value of {@link #operatingSystem} on macOS. */
  public static final char MAC = 'm';
  /** Value of {@link #operatingSystem} on Windows. */
  public static final char WIN = 'w';
  /** Value of {@link #operatingSystem} on BSD systems. */
  public static final char BSD = 'b';
  /** Value of {@link #operatingSystem} on Solaris. */
  public static final char SOLARIS = 's';
  /** Value of {@link #operatingSystem} on IBM i. */
  public static final char IBMI = 'i';
  /** Value of {@link #operatingSystem} on AIX. */
  public static final char AIX = 'a';

  static {
    final String os = operatingSystemName.toLowerCase(Locale.ROOT);
    if (os.contains("linux"))
      operatingSystem = LIN;
    else if (os.contains("mac"))
      operatingSystem = MAC;
    else if (os.contains("windows"))
      operatingSystem = WIN;
    else if (os.contains("bsd"))
      operatingSystem = BSD;
    else if (os.contains("sunos"))
      operatingSystem = SOLARIS;
    else if (os.contains("os/400") || os.contains("os400"))
      operatingSystem = IBMI;
    else if (os.contains("aix"))
      operatingSystem = AIX;
    else
      throw new UnsupportedOperatingSystemException("directories are not supported on " + operatingSystemName);
  }

}
