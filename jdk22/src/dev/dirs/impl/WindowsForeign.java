package dev.dirs.impl;

// based on https://github.com/dirs-dev/directories-jvm/blob/b76e3606c01e6f55b354e32a30e85ee6b9f815ca/src/main/java/dev/dirs/impl/Windows.java

import dev.dirs.Constants;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;

public final class WindowsForeign implements Windows {

  public WindowsForeign() {}

  static {
    if (Constants.operatingSystem == 'w') {
      System.loadLibrary("ole32");
      System.loadLibrary("shell32");
    }
  }

  private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup().or(Linker.nativeLinker().defaultLookup());
  private static final ValueLayout.OfByte C_CHAR = ValueLayout.JAVA_BYTE;
  private static final ValueLayout.OfShort C_SHORT = ValueLayout.JAVA_SHORT;
  private static final AddressLayout C_POINTER = ValueLayout.ADDRESS
      .withTargetLayout(MemoryLayout.sequenceLayout(java.lang.Long.MAX_VALUE, JAVA_BYTE));
  private static final ValueLayout.OfInt C_LONG = ValueLayout.JAVA_INT;

  private static String getDir(String folderId) {
    try (var arena = Arena.ofConfined()) {
      MemorySegment guidSegment = arena.allocate(GUID_LAYOUT);
      if (CLSIDFromString(createSegmentFromString(folderId, arena), guidSegment) != 0) {
        throw new AssertionError("failed converting string " + folderId + " to KnownFolderId");
      }
      MemorySegment path = arena.allocate(C_POINTER);
      path.set(C_POINTER, 0, MemorySegment.NULL);
      int hr = SHGetKnownFolderPath(guidSegment, 0, MemorySegment.NULL, path);
      MemorySegment pszPath = path.get(C_POINTER, 0);
      try {
        // On failure, return "", like WindowsPowerShell does
        if (hr != 0 || pszPath.equals(MemorySegment.NULL))
          return "";
        return createStringFromSegment(pszPath);
      } finally {
        // The buffer must be freed even if SHGetKnownFolderPath failed
        // (CoTaskMemFree accepts NULL, but there's no need to call it then)
        if (!pszPath.equals(MemorySegment.NULL))
          CoTaskMemFree(pszPath);
      }
    }
  }

  public String[] winDirs(String... folderIds) {
    String[] values = new String[folderIds.length];
    for (int i = 0; i < folderIds.length; i += 1)
      values[i] = getDir("{" + folderIds[i] + "}");
    return values;
  }

  /**
   * Creates a memory segment as a copy of a Java string.
   * <p>
   * The memory segment contains a copy of the string (null-terminated, UTF-16/wide characters).
   * </p>
   *
   * @param str   the string to copy
   * @param arena the arena for the memory segment
   * @return the resulting memory segment
   */
  private static MemorySegment createSegmentFromString(String str, Arena arena) {
    // allocate segment (including space for terminating null)
    var segment = arena.allocate(JAVA_CHAR, str.length() + 1L);
    // copy characters
    segment.copyFrom(MemorySegment.ofArray(str.toCharArray()));
    return segment;
  }

  /**
   * Creates a copy of the string in the memory segment.
   * <p>
   * The string must be a null-terminated UTF-16 (wide character) string.
   * </p>
   *
   * @param segment the memory segment
   * @return copied string
   */
  private static String createStringFromSegment(MemorySegment segment) {
    var len = 0;
    while (segment.get(JAVA_CHAR, len) != 0) {
      len += 2;
    }

    return new String(segment.asSlice(0, len).toArray(JAVA_CHAR));
  }

  private static MemorySegment findOrThrow(String symbol) {
    return SYMBOL_LOOKUP.find(symbol)
        .orElseThrow(() -> new UnsatisfiedLinkError("unresolved symbol: " + symbol));
  }

  private static final GroupLayout GUID_LAYOUT = MemoryLayout.structLayout(
          C_LONG.withName("Data1"),
          C_SHORT.withName("Data2"),
          C_SHORT.withName("Data3"),
          MemoryLayout.sequenceLayout(8, C_CHAR).withName("Data4"))
      .withName("_GUID");

  /**
   * {@snippet lang=c :
   * extern HRESULT CLSIDFromString(LPCOLESTR lpsz, LPCLSID pclsid)
   * }
   */
  private static int CLSIDFromString(MemorySegment lpsz, MemorySegment pclsid) {
    var handle = CLSIDFromString.HANDLE;
    try {
      return (int) handle.invokeExact(lpsz, pclsid);
    } catch (Throwable throwable) {
      throw new AssertionError("failed to invoke `CLSIDFromString`", throwable);
    }
  }

  private static class CLSIDFromString {
    public static final FunctionDescriptor DESC = FunctionDescriptor.of(C_LONG, C_POINTER, C_POINTER);

    public static final MethodHandle HANDLE = Linker.nativeLinker()
        .downcallHandle(findOrThrow("CLSIDFromString"), DESC);
  }

  /**
   * {@snippet lang=c :
   * extern HRESULT SHGetKnownFolderPath(const KNOWNFOLDERID *const rfid, DWORD dwFlags, HANDLE hToken, PWSTR *ppszPath)
   * }
   */
  private static int SHGetKnownFolderPath(MemorySegment rfid, int dwFlags, MemorySegment hToken, MemorySegment ppszPath) {
    var handle = SHGetKnownFolderPath.HANDLE;
    try {
      return (int) handle.invokeExact(rfid, dwFlags, hToken, ppszPath);
    } catch (Throwable throwable) {
      throw new AssertionError("failed to invoke `SHGetKnownFolderPath`", throwable);
    }
  }

  private static class SHGetKnownFolderPath {
    public static final FunctionDescriptor DESC = FunctionDescriptor.of(C_LONG, C_POINTER, C_LONG, C_POINTER, C_POINTER);

    public static final MethodHandle HANDLE = Linker.nativeLinker()
        .downcallHandle(findOrThrow("SHGetKnownFolderPath"), DESC);
  }

  /**
   * {@snippet lang=c :
   * extern void CoTaskMemFree(LPVOID pv)
   * }
   */
  private static void CoTaskMemFree(MemorySegment pv) {
    var handle = CoTaskMemFree.HANDLE;
    try {
      handle.invokeExact(pv);
    } catch (Throwable throwable) {
      throw new AssertionError("failed to invoke `CoTaskMemFree`", throwable);
    }
  }

  private static class CoTaskMemFree {
    public static final FunctionDescriptor DESC = FunctionDescriptor.ofVoid(C_POINTER);

    public static final MethodHandle HANDLE = Linker.nativeLinker()
        .downcallHandle(findOrThrow("CoTaskMemFree"), DESC);
  }

}
