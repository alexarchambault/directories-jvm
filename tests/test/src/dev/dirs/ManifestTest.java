package dev.dirs;

import java.io.File;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ManifestTest {

  static Attributes manifestAttributes(Class<?> cls) throws Exception {
    File jar = new File(cls.getProtectionDomain().getCodeSource().getLocation().toURI());
    try (JarFile jarFile = new JarFile(jar)) {
      return jarFile.getManifest().getMainAttributes();
    }
  }

  @Test
  public void testManifest() throws Exception {
    Attributes attributes = manifestAttributes(BaseDirectories.class);
    assertEquals("dev.dirs", attributes.getValue("Automatic-Module-Name"));
    assertEquals("true", attributes.getValue("Multi-Release"));
  }
}
