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

  /** Value of {@link #operatingSystem} on an operating system this library doesn't support.
    * <p>
    * Creating {@link BaseDirectories}, {@link UserDirectories} or {@link ProjectDirectories} instances
    * on such a system throws {@link UnsupportedOperatingSystemException}.
    */
  public static final char UNKNOWN = '?';

  static {
    operatingSystem = operatingSystemCode(operatingSystemName);
  }

  /** Computes the operating system code ({@link #LIN}, {@link #MAC}, …, or {@link #UNKNOWN}) corresponding to an {@code os.name} value. */
  static char operatingSystemCode(String operatingSystemName) {
    if (operatingSystemName == null)
      return UNKNOWN;
    final String os = operatingSystemName.toLowerCase(Locale.ROOT);
    if (os.contains("linux"))
      return LIN;
    else if (os.contains("mac"))
      return MAC;
    else if (os.contains("windows"))
      return WIN;
    else if (os.contains("bsd"))
      return BSD;
    else if (os.contains("sunos"))
      return SOLARIS;
    else if (os.contains("os/400") || os.contains("os400"))
      return IBMI;
    else if (os.contains("aix"))
      return AIX;
    else
      return UNKNOWN;
  }

}
