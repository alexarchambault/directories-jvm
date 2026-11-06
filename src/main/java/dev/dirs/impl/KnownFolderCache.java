package dev.dirs.impl;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Thread-safe cache of Windows known folder GUID to path results.
 * <p>
 * Only successful results (non-null, non-empty paths) are cached.
 * </p>
 */
final class KnownFolderCache {

  private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

  private static String key(String guid) {
    return guid.trim().toUpperCase(Locale.ROOT);
  }

  /**
   * Returns the paths of the passed known folders, from the cache, or from {@code fetch}.
   * <p>
   * {@code fetch} is called at most once, with the (normalized and de-duplicated) GUIDs
   * that aren't in cache, and must return their paths, in the same order.
   * </p>
   */
  String[] get(String[] guids, Function<String[], String[]> fetch) {
    String[] results = new String[guids.length];
    Set<String> missing = new LinkedHashSet<>();
    for (int i = 0; i < guids.length; i++) {
      String key = key(guids[i]);
      String cached = cache.get(key);
      if (cached == null)
        missing.add(key);
      else
        results[i] = cached;
    }

    if (!missing.isEmpty()) {
      String[] missingGuids = missing.toArray(new String[0]);
      String[] fetched = fetch.apply(missingGuids);
      if (fetched.length != missingGuids.length)
        throw new IllegalStateException("Expected " + missingGuids.length + " known folder paths, got " + fetched.length);
      Map<String, String> fetchedMap = new HashMap<>();
      for (int i = 0; i < missingGuids.length; i++) {
        String path = fetched[i];
        fetchedMap.put(missingGuids[i], path);
        // Don't cache failures
        if (path != null && !path.isEmpty())
          cache.put(missingGuids[i], path);
      }
      for (int i = 0; i < guids.length; i++)
        if (results[i] == null)
          results[i] = fetchedMap.get(key(guids[i]));
    }

    return results;
  }

  void clear() {
    cache.clear();
  }
}
