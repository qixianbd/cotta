package net.sf.cotta.memory;

import net.sf.cotta.*;
import net.sf.cotta.io.OutputMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.*;

public class InMemoryFileSystem implements FileSystem {
  private Map createDirs = new HashMap();
  private Map createFiles = new HashMap();
  private PathSeparator separator;
  private ListingOrder order;
  private int fileInitialCapacity = 0;
  private int fileSizeIncrement = 16;

  public InMemoryFileSystem() {
    this(PathSeparator.Unix);
  }

  public InMemoryFileSystem(ListingOrder order) {
    this(PathSeparator.Unix, order);
  }

  public InMemoryFileSystem(PathSeparator separator) {
    this(separator, ListingOrder.NULL);
  }

  public InMemoryFileSystem(PathSeparator separator, ListingOrder order) {
    this.separator = separator;
    this.order = order;
  }

  public void setFileInitialCapacity(int value) {
    this.fileInitialCapacity = value;
  }

  public void setFileSizeIncrement(int value) {
    this.fileSizeIncrement = value;
  }

  public boolean fileExists(TPath path) {
    return createFiles.containsKey(path);
  }

  public void createFile(TPath path) throws TIoException {
    if (!dirExists(path.parent())) {
      throw new TIoException(path, "Parent not created");
    }
    getChildren(path.parent(), createDirs).addFile(path);
    createFileInSystem(path).setContent("");
  }

  public void createDir(TPath path) throws TIoException {
    if (dirExists(path)) {
      throw new IllegalArgumentException(path.toPathString() + " already exists");
    }
    if (fileExists(path)) {
      throw new TIoException(path, "already exists as a file");
    }
    ensureDirExists(path.parent()).addDir(path);
    createDirImpl(path);
  }

  private DirectoryContent ensureDirExists(TPath dir) throws TIoException {
    if (!dirExists(dir)) {
      createDir(dir);
    }
    return getChildren(dir, createDirs);
  }

  public void deleteFile(TPath path) throws TFileNotFoundException {
    if (!createFiles.containsKey(path)) {
      throw new TFileNotFoundException(path);
    }
    createFiles.remove(path);
    getChildren(path.parent(), createDirs).removeFile(path);
  }

  public boolean dirExists(TPath path) {
    if (createDirs.containsKey(path)) {
      return true;
    }
    if (path.parent() == null) {
      createDirImpl(path);
      return true;
    }
    return false;
  }

  private void createDirImpl(TPath path) {
    createDirs.put(path, new DirectoryContent());
  }

  public TPath[] listDirs(TPath path) {
    return sort(getChildren(path, createDirs).dirs());
  }

  private TPath[] sort(Collection dirs) {
    List collection = new ArrayList(dirs);
    collection = order.sort(collection);
    return (TPath[]) collection.toArray(new TPath[collection.size()]);
  }

  public TPath[] listFiles(TPath path) {
    return sort(getChildren(path, createDirs).files());
  }

  private DirectoryContent getChildren(TPath parent, Map collection) {
    return ((DirectoryContent) collection.get(parent));
  }

  public InputStream createInputStream(TPath path) throws TIoException {
    return retrieveFileContent(path).inputStream();
  }

  private FileContent retrieveFileContent(TPath path) throws TFileNotFoundException {
    FileContent content = fileContent(path);
    if (content == null) {
      throw new TFileNotFoundException(path);
    }
    return content;
  }

  private FileContent fileContent(TPath path) {
    return (FileContent) createFiles.get(path);
  }

  public OutputStream createOutputStream(TPath path, OutputMode mode) throws TIoException {
    FileContent content = fileContent(path);
    if (content == null) {
      content = createFileInSystem(path);
    }
    if (mode.isOverwrite()) {
      content.setContent("");
    }
    return content.outputStream();
  }

  public FileChannel createOutputChannel(TPath path, OutputStream outputStream) throws TIoException {
    return null;
  }

  private FileContent createFileInSystem(TPath path) throws TIoException {
    if (dirExists(path)) {
      throw new TIoException(path, "exists as a directory");
    }
    if (!dirExists(path.parent())) {
      throw new TIoException(path, "parent needs to be created first:");
    }
    getChildren(path.parent(), createDirs).addFile(path);
    FileContent fileContent = new FileContent(fileInitialCapacity, fileSizeIncrement);
    createFiles.put(path, fileContent);
    return fileContent;
  }

  public void deleteDirectory(TPath path) throws TIoException {
    if (!dirExists(path)) {
      throw new TDirectoryNotFoundException(path);
    }
    DirectoryContent directoryContent = getChildren(path, createDirs);
    if (!directoryContent.isEmpty()) {
      throw new TIoException(path, "Directory not empty");
    }
    createDirs.remove(path);
    getChildren(path.parent(), createDirs).removeDir(path);
  }

  public void moveFile(TPath source, TPath destination) throws TIoException {
    FileContent sourceFile = fileContent(source);
    FileContent destFile = createFileInSystem(destination);
    destFile.setContent(sourceFile.getContent());
    deleteFile(source);
  }

  public void moveDirectory(TPath source, TPath destination) throws TIoException {
    createDir(destination);
    moveSubDirectories(source, destination);
    moveFiles(source, destination);
    deleteDirectory(source);
  }

  private void moveSubDirectories(TPath source, TPath destination) throws TIoException {
    TPath[] directories = listDirs(source);
    for (int i = 0; i < directories.length; i++) {
      TPath directory = directories[i];
      moveDirectory(directory, destination.join(directory.lastElementName()));
    }
  }

  private void moveFiles(TPath source, TPath destination) throws TIoException {
    TPath[] files = listFiles(source);
    for (int i = 0; i < files.length; i++) {
      TPath file = files[i];
      moveFile(file, destination.join(file.lastElementName()));
    }
  }

  public String pathString(TPath path) {
    return path.toPathString(separator);
  }

  public long fileLength(TPath path) {
    return fileContent(path).content.size();
  }

  public File toJavaFile(TPath path) {
    throw new UnsupportedOperationException("InMemoryFileSystem");
  }

  public String toCanonicalPath(TPath path) {
    return "memory://" + pathString(path);
  }

  public FileChannel createInputChannel(TPath path) throws TFileNotFoundException {
    return retrieveFileContent(path).inputChannel();
  }

  private static class DirectoryContent {
    private Map dirs = new HashMap();
    private Map files = new HashMap();

    public Collection dirs() {
      return dirs.values();
    }

    public void addDir(TPath directory) {
      dirs.put(directory.lastElementName(), directory);
    }

    public void addFile(TPath file) {
      files.put(file.lastElementName(), file);
    }

    public Collection files() {
      return files.values();
    }

    public boolean isEmpty() {
      return files.isEmpty() && dirs.isEmpty();
    }

    public void removeFile(TPath file) {
      files.remove(file.lastElementName());
    }

    public void removeDir(TPath directory) {
      dirs.remove(directory.lastElementName());
    }
  }

  private static class FileContent {
    private ByteArrayBuffer content;
    private int increament;

    public FileContent(int initialCapacity, int increament) {
      content = new ByteArrayBuffer(initialCapacity, increament);
      this.increament = increament;
    }

    public void setContent(String content) {
      this.content = new ByteArrayBuffer(content.getBytes(), increament);
    }

    public String getContent() {
      return new String(content.toByteArray());
    }

    public OutputStream outputStream() {
      return new OutputStream() {

        public void write(int b) {
          content.append((byte) b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
          content.append(b, off, len);
        }

        public void write(byte[] b) throws IOException {
          content.append(b);
        }
      };
    }

    public InputStream inputStream() {
      return new InputStream() {
        private int position = 0;

        public int read() {
          return (position == content.size()) ? -1 : content.byteAt(position++) & 0xFF;
        }
      };
    }

    public FileChannel inputChannel() {
      return new InMemoryInputFileChannel(content);
    }

  }


}