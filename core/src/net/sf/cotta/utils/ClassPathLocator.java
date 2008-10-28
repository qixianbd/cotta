package net.sf.cotta.utils;

import java.net.URL;

/**
 * @deprecated use ClassPathEntryLocator
 */
public class ClassPathLocator extends ClassPathEntryLocator {
  public ClassPathLocator(Class clazz) {
    super(clazz);
  }

  public ClassPathLocator(String absoluteResourcePath) {
    super(absoluteResourcePath);
  }

  @SuppressWarnings({"deprecation"})
  public ClassPath locate() {
    URL url = getClass().getResource(resourceString);
    if ("jar".equalsIgnoreCase(url.getProtocol())) {
      return new ClassPath(getJarFileOnClassPath(url));
    } else {
      return new ClassPath(goToClassPathRootDirectory(url));
    }
  }
}