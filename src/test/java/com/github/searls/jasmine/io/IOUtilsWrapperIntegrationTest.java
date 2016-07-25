package com.github.searls.jasmine.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IOUtils.class, FileUtils.class})
public class IOUtilsWrapperIntegrationTest {
  private final IOUtilsWrapper subject = new IOUtilsWrapper();
  private final InputStream inputStream = mock(InputStream.class);
  private final File file = mock(File.class);

  @Before
  public void powerfullyMockStaticClasses() {
    mockStatic(IOUtils.class);
    mockStatic(FileUtils.class);
  }

  @Test
  public void shouldDelegateToString() throws IOException {
    String expected = "pants";
    when(IOUtils.toString(inputStream)).thenReturn(expected);

    String result = subject.toString(inputStream);

    assertThat(result, is(expected));
  }

  @Test
  public void shouldDelegateResourceStringsToString() throws IOException {
    String expected = "banana";
    when(IOUtils.toString(isA(InputStream.class))).thenReturn(expected);

    String result = subject.toString("/ioUtils.txt");

    assertThat(result, is(expected));
  }

  @Test
  public void shouldDelegateFileToString() throws IOException {
    String expected = "foo";
    when(FileUtils.readFileToString(file)).thenReturn(expected);

    String result = subject.toString(file);

    assertThat(result, is(expected));
  }
}
