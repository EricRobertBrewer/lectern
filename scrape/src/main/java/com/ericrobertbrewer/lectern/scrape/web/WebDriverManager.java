package com.ericrobertbrewer.lectern.scrape.web;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;

/**
 * Used to be able to quit and reopen the browser, for example, when being throttled by a server.
 */
public class WebDriverManager implements AutoCloseable {

  public enum Browser {
    CHROME,
    EDGE,
    FIREFOX,
    SAFARI
  }

  private final Browser browser;
  private WebDriver driver = null;

  public WebDriverManager(Browser browser) {
    this.browser = browser;
  }

  public WebDriver getDriver() {
    if (driver == null) {
      switch (browser) {
        case CHROME:
          driver = new ChromeDriver();
          break;
        case EDGE:
          driver = new EdgeDriver();
          break;
        case FIREFOX:
          driver = new FirefoxDriver();
          break;
        case SAFARI:
          driver = new SafariDriver();
          break;
      }
    }
    return driver;
  }

  @Override
  public void close() {
    if (driver != null) {
      driver.quit();
      driver = null;
    }
  }
}
