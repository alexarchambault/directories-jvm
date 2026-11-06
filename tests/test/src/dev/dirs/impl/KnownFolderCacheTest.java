package dev.dirs.impl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class KnownFolderCacheTest {

  /** Fake fetcher, returning "path-of-GUID", or "" for GUIDs starting with "BAD" */
  private static final class Fetcher implements Function<String[], String[]> {
    final List<List<String>> calls = new ArrayList<>();
    RuntimeException toThrow = null;

    @Override
    public synchronized String[] apply(String[] guids) {
      calls.add(Arrays.asList(guids));
      if (toThrow != null)
        throw toThrow;
      String[] res = new String[guids.length];
      for (int i = 0; i < guids.length; i++)
        res[i] = guids[i].startsWith("BAD") ? "" : "path-of-" + guids[i];
      return res;
    }
  }

  @Test
  public void cachesResults() {
    KnownFolderCache cache = new KnownFolderCache();
    Fetcher fetcher = new Fetcher();

    assertArrayEquals(new String[] { "path-of-A", "path-of-B" }, cache.get(new String[] { "A", "B" }, fetcher));
    assertEquals(Arrays.asList(Arrays.asList("A", "B")), fetcher.calls);

    // all in cache: no new fetch
    assertArrayEquals(new String[] { "path-of-B", "path-of-A" }, cache.get(new String[] { "B", "A" }, fetcher));
    assertEquals(1, fetcher.calls.size());

    // only the missing ones are fetched, in one call, without duplicates
    assertArrayEquals(
        new String[] { "path-of-C", "path-of-A", "path-of-D", "path-of-C" },
        cache.get(new String[] { "C", "A", "D", "C" }, fetcher));
    assertEquals(Arrays.asList(Arrays.asList("A", "B"), Arrays.asList("C", "D")), fetcher.calls);
  }

  @Test
  public void keysAreCaseInsensitive() {
    KnownFolderCache cache = new KnownFolderCache();
    Fetcher fetcher = new Fetcher();
    String guid = "3eb685db-65f9-4cf6-a03a-e3ef65729f3d";
    String[] first = cache.get(new String[] { guid }, fetcher);
    String[] second = cache.get(new String[] { guid.toUpperCase() }, fetcher);
    assertArrayEquals(first, second);
    assertEquals(1, fetcher.calls.size());
  }

  @Test
  public void emptyRequest() {
    KnownFolderCache cache = new KnownFolderCache();
    Fetcher fetcher = new Fetcher();
    assertArrayEquals(new String[0], cache.get(new String[0], fetcher));
    assertTrue(fetcher.calls.isEmpty());
  }

  @Test
  public void doesntCacheFailedLookups() {
    KnownFolderCache cache = new KnownFolderCache();
    Fetcher fetcher = new Fetcher();
    assertArrayEquals(new String[] { "", "path-of-A" }, cache.get(new String[] { "BAD1", "A" }, fetcher));
    assertArrayEquals(new String[] { "", "path-of-A" }, cache.get(new String[] { "BAD1", "A" }, fetcher));
    assertEquals(Arrays.asList(Arrays.asList("BAD1", "A"), Arrays.asList("BAD1")), fetcher.calls);
  }

  @Test
  public void doesntCacheExceptions() {
    KnownFolderCache cache = new KnownFolderCache();
    Fetcher fetcher = new Fetcher();
    fetcher.toThrow = new RuntimeException("PowerShell failed");
    try {
      cache.get(new String[] { "A" }, fetcher);
      fail("Expected an exception");
    } catch (RuntimeException e) {
      assertEquals("PowerShell failed", e.getMessage());
    }
    fetcher.toThrow = null;
    assertArrayEquals(new String[] { "path-of-A" }, cache.get(new String[] { "A" }, fetcher));
    assertEquals(2, fetcher.calls.size());
  }

  @Test
  public void clear() {
    KnownFolderCache cache = new KnownFolderCache();
    Fetcher fetcher = new Fetcher();
    cache.get(new String[] { "A" }, fetcher);
    cache.clear();
    cache.get(new String[] { "A" }, fetcher);
    assertEquals(2, fetcher.calls.size());
  }

  @Test
  public void concurrentAccess() throws InterruptedException {
    KnownFolderCache cache = new KnownFolderCache();
    Fetcher fetcher = new Fetcher();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread[] threads = new Thread[8];
    for (int t = 0; t < threads.length; t++) {
      final int t0 = t;
      threads[t] = new Thread(() -> {
        try {
          for (int i = 0; i < 1000; i++) {
            String a = "G" + (i % 17);
            String b = "G" + ((i + t0) % 23);
            String[] res = cache.get(new String[] { a, b }, fetcher);
            if (!res[0].equals("path-of-" + a) || !res[1].equals("path-of-" + b))
              throw new AssertionError("Unexpected result " + Arrays.toString(res) + " for " + a + ", " + b);
          }
        } catch (Throwable e) {
          failure.compareAndSet(null, e);
        }
      });
      threads[t].start();
    }
    for (Thread thread : threads)
      thread.join();
    if (failure.get() != null)
      throw new AssertionError(failure.get());
  }
}
