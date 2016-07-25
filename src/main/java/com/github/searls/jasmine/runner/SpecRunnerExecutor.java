package com.github.searls.jasmine.runner;

import com.github.searls.jasmine.io.IOUtilsWrapper;
import com.github.searls.jasmine.model.JasmineResult;
import com.google.common.base.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class SpecRunnerExecutor {

  public static final String BUILD_REPORT_JS = "/lib/buildReport.js";
  public static final String CREATE_JUNIT_XML = "/lib/createJunitXml.js";

  private final IOUtilsWrapper ioUtilsWrapper;

  public SpecRunnerExecutor(IOUtilsWrapper ioUtilsWrapper) {
    this.ioUtilsWrapper = ioUtilsWrapper;
  }

  public SpecRunnerExecutor() {
    this(new IOUtilsWrapper());
  }


  public JasmineResult execute(URL runnerUrl, File junitXmlReport, WebDriver driver, int timeout, boolean debug, Log log, String format, File customReporter) {
    try {
      if (!(driver instanceof JavascriptExecutor)) {
        throw new RuntimeException("The provided web driver can't execute JavaScript: " + driver.getClass());
      }
      JavascriptExecutor executor = (JavascriptExecutor) driver;
      driver.get(runnerUrl.toString());
      this.waitForRunnerToFinish(driver, timeout, debug, log);

      this.checkForConsoleErrors(driver, log);

      JasmineResult jasmineResult = new JasmineResult();
      jasmineResult.setDetails(this.buildReport(executor, format, customReporter));
      FileUtils.writeStringToFile(junitXmlReport, this.buildJunitXmlReport(executor, debug), "UTF-8");

      return jasmineResult;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
        driver.quit();
      } catch (Exception e) {
        log.error("There was an exception quitting WebDriver.", e);
      }
    }
  }

  private void checkForConsoleErrors(WebDriver driver, Log log) {
    WebElement head = driver.findElement(By.tagName("head"));
    if (head != null) {
      String jserrors = head.getAttribute("jmp_jserror");
      if (StringUtils.isNotBlank(jserrors)) {
        log.warn("JavaScript Console Errors:\n\n  * " + jserrors.replaceAll(":!:", "\n  * ") + "\n\n");
        throw new RuntimeException("There were javascript console errors.");
      }
    }
  }

  private String buildReport(JavascriptExecutor driver, String format, File customReporter) throws IOException {
    String script =
      readReporterOrDefault(customReporter) +
        "return jasmineMavenPlugin.printReport(window.jsApiReporter,{format:'" + format + "'});";
    Object report = driver.executeScript(script);
    return report.toString();
  }

  private String readReporterOrDefault(File reporter) throws IOException {
    if (null != reporter) {
      return this.ioUtilsWrapper.toString(reporter);
    } else {
      return this.ioUtilsWrapper.toString(BUILD_REPORT_JS);
    }
  }

  private String buildJunitXmlReport(JavascriptExecutor driver, boolean debug) throws IOException {
    Object junitReport = driver.executeScript(
      this.ioUtilsWrapper.toString(CREATE_JUNIT_XML) +
        "return junitXmlReporter.report(window.jsApiReporter," + debug + ");");
    return junitReport.toString();
  }

  private void waitForRunnerToFinish(final WebDriver driver, int timeout, boolean debug, Log log) throws InterruptedException {
    final JavascriptExecutor executor = (JavascriptExecutor) driver;
    new WebDriverWait(driver, timeout, 1000).until(new Predicate<WebDriver>() {
      @Override
      public boolean apply(WebDriver input) {
        return SpecRunnerExecutor.this.executionFinished(executor);
      }
    });

    if (!this.executionFinished(executor)) {
      this.handleTimeout(timeout, debug, log);
    }
  }

  private void handleTimeout(int timeout, boolean debug, Log log) {
    log.warn("Attempted to wait for your specs to finish processing over the course of " +
      timeout +
      " seconds, but it still appears to be running.");
    if (debug) {
      log.warn("Debug mode: will attempt to parse the incomplete spec runner results");
    } else {
      throw new IllegalStateException("Timeout occurred. Aborting execution of specs. (Try configuring 'debug' to 'true' for more details.)");
    }
  }

  private Boolean executionFinished(JavascriptExecutor driver) {
    return (Boolean) driver.executeScript("return (window.jsApiReporter === undefined) ? false : window.jsApiReporter.finished");
  }

}
