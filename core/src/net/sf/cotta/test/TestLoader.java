package net.sf.cotta.test;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.cotta.TDirectory;
import net.sf.cotta.TFile;
import net.sf.cotta.TFileFilter;
import net.sf.cotta.TIoException;
import net.sf.cotta.utils.ClassCollector;
import net.sf.cotta.utils.ClassPathEntryLocator;
import net.sf.cotta.utils.ClassPathEntryProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestLoader {
  private Class resoureClass;
  private List<String> classNames = new ArrayList<String>();

  public TestLoader(Class resoureClass) {
    this.resoureClass = resoureClass;
  }

  public TestSuite loadTests() {
    try {
      new ClassPathEntryLocator(resoureClass).locateEntry().read(collectNames());
    } catch (TIoException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    TestSuite testSuite = new TestSuite();
    for (String name : classNames) {
      testSuite.addTestSuite(loadClass(name));
    }
    return testSuite;
  }

  private Class<? extends TestCase> loadClass(String name) {
    Class<? extends TestCase> testClass;
    try {
      Class<?> loadedClass = Class.forName(name);
      //noinspection unchecked
      testClass = (Class<? extends TestCase>) loadedClass;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return testClass;
  }

  private ClassPathEntryProcessor collectNames() {
    return new ClassPathEntryProcessor() {
      public void process(TDirectory directory) throws TIoException {
        classNames = new ClassCollector(directory, "", classEndsWithTest()).collectNames();
        Collections.sort(classNames);
      }
    };
  }

  private TFileFilter classEndsWithTest() {
    return new TFileFilter() {
      public boolean accept(TFile file) {
        return file.name().endsWith("Test.class");
      }
    };
  }
}