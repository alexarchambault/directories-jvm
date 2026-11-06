package dev.dirs.jni;

import java.io.File;
import java.util.jar.JarFile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ManifestJniTest {

  @Test
  public void testManifest() throws Exception {
    File jar = new File(WindowsJni.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    try (JarFile jarFile = new JarFile(jar)) {
      assertEquals("dev.dirs.jni", jarFile.getManifest().getMainAttributes().getValue("Automatic-Module-Name"));
    }
  }
}
