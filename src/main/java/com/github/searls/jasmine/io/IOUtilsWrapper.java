package com.github.searls.jasmine.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class IOUtilsWrapper {

  public String toString(File file) throws IOException {
    return FileUtils.readFileToString(file);
  }

  public String toString(InputStream inputStream) throws IOException {
    return IOUtils.toString(inputStream);
  }

  public String toString(String name) throws IOException {
    return this.toString(this.getClass().getResourceAsStream(name));
  }

}
