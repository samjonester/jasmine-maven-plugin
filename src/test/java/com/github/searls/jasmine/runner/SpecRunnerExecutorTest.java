package com.github.searls.jasmine.runner;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.github.searls.jasmine.io.IOUtilsWrapper;
import com.github.searls.jasmine.model.JasmineResult;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtils.class)
public class SpecRunnerExecutorTest {

  private static final String BUILD_REPORT_JS_CONTENTS = "var jasmineMavenPlugin = {printReport: function(){ return 'pants\\nkaka'; }};";
  private static final String BUILD_CUSTOM_REPORT_JS_CONTENTS = "var jasmineMavenPlugin = {printReport: function(){ return 'hello\\nworld'; }};";
  private static final String JUNIT_RESULTS = "var junitXmlReporter = { report: function(reporter) { return '<xml/>'; }};";
  private static HtmlUnitDriver driver;

  @Mock
  private IOUtilsWrapper ioUtilsWrapper;

  @Mock
  private File file;
  @Mock
  private Log log;

  private final URL resource = this.getClass().getResource("/example_nested_specrunner.html");

  @InjectMocks
  private SpecRunnerExecutor subject;

  @Before
  public void stubResourceStreams() throws IOException {
    spy(FileUtils.class);

    when(this.ioUtilsWrapper.toString(isA(String.class))).thenReturn(BUILD_REPORT_JS_CONTENTS, JUNIT_RESULTS);
    driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_38);
    driver.setJavascriptEnabled(true);
  }

  @Test
  public void shouldFindSpecsInResults() throws Exception {
    doNothing().when(FileUtils.class);
    FileUtils.writeStringToFile(this.file, "<xml/>", "UTF-8");

    JasmineResult result = this.subject.execute(this.resource, this.file, driver, 300, false, this.log, null, null);

    assertThat(result, is(not(nullValue())));
    assertThat(result.getDescription(), containsString("kaka"));
    assertThat(result.getDetails(), containsString("pants"));
    assertThat(result.didPass(), is(false));

    verifyStatic();
    FileUtils.writeStringToFile(this.file, "<xml/>", "UTF-8");
  }

  @Test
  public void shouldFindSpecsInResultsWithCustomReporter() throws Exception {
    doNothing().when(FileUtils.class);
    FileUtils.writeStringToFile(this.file, "<xml/>", "UTF-8");
    File customReporter = mock(File.class);
    when(this.ioUtilsWrapper.toString(customReporter)).thenReturn(BUILD_CUSTOM_REPORT_JS_CONTENTS);
    when(this.ioUtilsWrapper.toString(isA(String.class))).thenReturn(JUNIT_RESULTS);

    JasmineResult result = this.subject.execute(this.resource, this.file, driver, 300, false, this.log, null, customReporter);

    assertThat(result, is(not(nullValue())));
    assertThat(result.getDescription(), containsString("world"));
    assertThat(result.getDetails(), containsString("hello"));
    assertThat(result.didPass(), is(false));

    verifyStatic();
    FileUtils.writeStringToFile(this.file, "<xml/>", "UTF-8");
  }

  @Test
  public void shouldExportJUnitResults() throws Exception {
    doNothing().when(FileUtils.class);
    FileUtils.writeStringToFile(this.file, "<xml/>", "UTF-8");

    this.subject.execute(this.resource, this.file, driver, 300, false, this.log, null, null);

    verifyStatic();
    FileUtils.writeStringToFile(this.file, "<xml/>", "UTF-8");
  }
}
