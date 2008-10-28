package net.sf.cotta;

import net.sf.cotta.io.*;
import net.sf.cotta.memory.InMemoryFileSystem;
import org.jmock.Expectations;
import org.jmock.Mockery;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class TFileTest extends CottaTestBase {

  public void testBeCreatedWithCorrectNameAndNotExists() throws Exception {
    TFile file = file("name.txt");
    ensure.that(file.exists()).eq(false);
    ensure.that(file.name()).eq("name.txt");
  }

  public void testExistWithEmptyContentAfterCreateInvoked() throws Exception {
    TFile file = file("test.txt");
    ensure.that(file.exists()).eq(false);
    file.create();
    ensureEquals(file.exists(), true);
    ensureEquals(file.load(), "");
  }

  public void testCreateParentAsWellDuringCreation() throws Exception {
    TFile file = file("test/test.txt");
    file.create();
    ensure.that(file.parent().exists()).eq(true);
  }

  private TFile file(String path) {
    TFileFactory factory = new TFileFactory(new InMemoryFileSystem());
    return factory.file(path);
  }

  public void testBeAbleToProvideParent() throws Exception {
    TFile file = file("/tmp/test.txt");
    TDirectory directory = file.parent();
    ensureEquals(directory.name(), "tmp");
    ensureEquals(directory.exists(), false);
  }

  public void testWriteToAndReadFromFile() throws Exception {
    TFile file = file("/tmp/test.txt");
    file.save("content");
    ensureEquals(file.load(), "content");
  }

  public void testBeEqualIfPathAndFileSystemAreEqual() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    TFile fileOne = new TFile(fileSystem, TPath.parse("/tmp/test"));
    TFile fileTwo = new TFile(fileSystem, TPath.parse("/tmp/test"));
    ensure.that(fileOne.equals(fileTwo)).eq(true);
  }

  public void testCopyToAnotherFile() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    String content = "This is a sample content";
    TFile source = new TFile(fileSystem, TPath.parse("/source.txt"));
    source.save(content);
    TFile dest = new TFile(fileSystem, TPath.parse("/dest.txt"));
    source.copyTo(dest);
    ensure.that(dest.exists()).eq(true);
    ensure.that(dest.load()).eq(content);
  }

  public void testCallMoveDirectlyIfUsingSameFileSystem() throws Exception {
    Mockery context = new Mockery();
    final FileSystem fileSystem = context.mock(FileSystem.class);
    final TPath sourcePath = TPath.parse("/source.txt");
    TFile source = new TFile(fileSystem, sourcePath);
    final TPath destPath = TPath.parse("/dest.txt");
    TFile dest = new TFile(fileSystem, destPath);
    context.checking(new Expectations() {
      {
        one(fileSystem).moveFile(sourcePath, destPath);
        one(fileSystem).fileExists(sourcePath);
        will(returnValue(true));
        one(fileSystem).fileExists(destPath);
        will(returnValue(false));
      }
    });
    source.moveTo(dest);
    context.assertIsSatisfied();
  }

  public void testCheckSourceDuringMove() throws Exception {
    TFileFactory factory = new TFileFactory(new InMemoryFileSystem());
    TFile source = factory.file("source.txt");
    TFile dest = factory.file("dest.txt");
    try {
      source.moveTo(dest);
      fail("TFileNotFoundException should have been thrown");
    } catch (TFileNotFoundException e) {
      ensure.that(e.getPath()).eq(TPath.parse("source.txt"));
    }
  }

  public void testCheckDestinationDuringMove() throws Exception {
    TFileFactory factory = new TFileFactory(new InMemoryFileSystem());
    TFile source = factory.file("source.txt");
    source.save("content");
    TFile dest = factory.file("dest.txt");
    dest.save("content 2");
    try {
      source.moveTo(dest);
      fail("TIoException should have been thrown");
    } catch (TIoException e) {
      ensure.that(e.getPath()).eq(TPath.parse("dest.txt"));
    }
  }

  public void testCopyThenDeleteIfDifferentFileSystem() throws Exception {
    String content = "move file behaviour";
    InMemoryFileSystem source = new InMemoryFileSystem();
    InMemoryFileSystem dest = new InMemoryFileSystem();
    TFile sourceFile = new TFile(source, TPath.parse("/source.txt"));
    TFile destFile = new TFile(dest, TPath.parse("/dest.txt"));
    sourceFile.save(content);
    sourceFile.moveTo(destFile);
    ensure.that(sourceFile.exists()).eq(false);
    ensure.that(destFile.exists()).eq(true);
    ensure.that(destFile.load()).eq(content);
  }

  public void testEnsureExists() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    TFile file = new TFile(fileSystem, TPath.parse("/source.txt")).ensureExists();
    ensure.that(file.load()).eq("");
  }

  public void testKnowExteionAndBaseName() throws Exception {
    TFile file = new TFile(new InMemoryFileSystem(), TPath.parse("/tmp/source/content.txt"));
    ensure.that("txt").eq(file.extname());
    ensure.that("content").eq(file.basename());
  }

  public void testReturnEmptyStringForNoExtensionName() {
    TFile file = new TFile(new InMemoryFileSystem(), TPath.parse("/tmp/source/run_all"));
    ensure.that("").eq(file.extname());
    ensure.that("run_all").eq(file.basename());
  }

  public void testReturnEmptyStringForNoBaseName() throws Exception {
    TFile file = new TFile(new InMemoryFileSystem(), TPath.parse("/tmp/.vimrc"));
    ensure.that("vimrc").eq(file.extname());
    ensure.that("").eq(file.basename());
  }

  public void testPassBackFileIoAndCloseResource() throws Exception {
    TPath path = TPath.parse("/tmp/test.txt");
    Mockery context = new Mockery();
    FileSystem fileSystem = context.mock(FileSystem.class);
    TFile file = new TFile(fileSystem, path);
    final IoProcessor ioProcessor = context.mock(IoProcessor.class);
    context.checking(new Expectations() {
      {
        one(ioProcessor).process(with(aNonNull(IoManager.class)));
      }
    });
    file.open(ioProcessor);
    context.assertIsSatisfied();
  }

  public void testProcessFileIoAndCloseResource() throws Exception {
    final InputStreamStub inputStream = new InputStreamStub();
    Mockery context = new Mockery();
    final FileSystem fileSystem = context.mock(FileSystem.class);
    final TPath path = TPath.parse("/tmp/test.txt");
    TFile file = new TFile(fileSystem, path);
    context.checking(new Expectations() {
      {
        one(fileSystem).createInputStream(path);
        will(returnValue(inputStream));
      }
    });
    file.open(new IoProcessor() {
      public void process(IoManager io) throws IOException {
        io.inputStream();
      }
    });
    ensure.that(inputStream.isClosed()).eq(true);
    context.assertIsSatisfied();
  }

  public void testOpenFileForRead() throws Exception {
    //Given
    TFile file = file("/tmp/read.txt");
    file.save("dvorak is cool");
    final StringBuffer buffer = new StringBuffer();
    //When
    file.read(new InputProcessor() {
      public void process(InputManager inputManager) throws IOException {
        buffer.append(inputManager.bufferedReader().readLine());
      }
    });
    //Ensure
    String actualLineThatRead = buffer.toString();
    ensure.that(actualLineThatRead).eq("dvorak is cool");
  }

  public void testAppendFile() throws Exception {
    //Given
    TFile file = file("/tmp/append.txt");
    file.save("one");
    //When
    file.append(new OutputProcessor() {
      public void process(OutputManager outputManager) throws IOException {
        outputManager.bufferedWriter().write("two");
      }
    });
    //Ensure
    ensure.that(file.load()).eq("onetwo");
  }

  public void testLoadFileContent() throws Exception {
    TFileFactory factory = new TFileFactory(new InMemoryFileSystem());
    TFile file = factory.file("/tmp/read.txt");
    file.create();
    ensure.that(file.load()).eq("");
  }

  public void testSaveToFile() throws Exception {
    TFileFactory factory = new TFileFactory(new InMemoryFileSystem());
    TFile file = factory.file("/tmp/working.txt");
    TFile returned = file.save("my content");
    ensure.that(returned).sameAs(file);
    ensure.that(file.load()).eq("my content");
  }

  public void testProvideRightTPathForIo() throws Exception {
    final TPath path = TPath.parse("expected");
    Mockery context = new Mockery();
    final FileSystem fileSystem = context.mock(FileSystem.class);
    context.checking(new Expectations() {
      {
        one(fileSystem).createInputStream(path);
        will(returnValue(new ByteArrayInputStream(new byte[0])));
      }
    });
    TFileFactory factory = new TFileFactory(fileSystem);
    TFile file = factory.file(path.toPathString());
    try {
      file.open(new IoProcessor() {
        public void process(IoManager io) throws IOException {
          io.inputStream();
          throw new IOException("test");
        }
      });
      fail("TIoException should have been thrown");
    }
    catch (TIoException e) {
      ensure.that(e.getPath()).eq(path);
      ensure.that(e.getCause().getMessage()).eq("test");
    }
    context.assertIsSatisfied();
  }

  public void testGetJavaFileObject() {
    final File expected = new File("file.txt");
    final TPath path = TPath.parse("file.txt");
    Mockery context = new Mockery();
    final FileSystem fileSystem = context.mock(FileSystem.class);
    context.checking(new Expectations() {
      {
        one(fileSystem).toJavaFile(path);
        will(returnValue(expected));
      }
    });
    TFile file = new TFile(fileSystem, path);
    ensure.that(file.toJavaFile()).sameAs(expected);
    context.assertIsSatisfied();
  }

  public void testExposePathBehaviours() throws Exception {
    TDirectory directory = new TDirectory(new InMemoryFileSystem(), TPath.parse("/one/two"));
    TFile file = directory.file(TPath.parse("three/four.txt"));
    ensure.that(file.isChildOf(directory)).eq(true);
    ensure.that(file.pathFrom(directory)).eq(file.toPath().pathFrom(directory.toPath()));
  }

}