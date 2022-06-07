package com.ericrobertbrewer.lectern.scrape.web;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 * Used to be able to quit and reopen the browser, for example, when being throttled by a server.
 */
public class WebDriverManager implements AutoCloseable {

  public enum Browser {
    CHROME,
    FIREFOX,
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
        case FIREFOX:
          driver = new FirefoxDriver();
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

  public WebElement navigateAndFindElement(String url, By by, int retries) {
    retries = Math.max(0, retries);
    while (true) {
      try {
        getDriver().navigate().to(url);
        return getDriver().findElement(by);
      } catch (NoSuchElementException e) {
        if (retries > 0) {
          close();
          retries--;
        } else {
          throw e;
        }
      }
    }
  }
}
