package com.ericrobertbrewer.lectern.web;

import org.openqa.selenium.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class WebUtils {

  private WebUtils() {
  }

  public static String appendQuery(String url, String key, String value) {
    final String append =
      URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    try {
      final URI uri = new URI(url);
      final String query = uri.getQuery() != null ? uri.getQuery() + "&" + append : append;
      return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment()).toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static WebElement findElementOrNull(SearchContext context, By by) {
    try {
      return context.findElement(by);
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  public static WebElement findElement(SearchContext context, By[] bys) {
    for (int i = 0; i < bys.length - 1; i++) {
      try {
        return context.findElement(bys[i]);
      } catch (NoSuchElementException ignored) {
      }
    }
    return context.findElement(bys[bys.length-1]);
  }

  public static WebElement navigateAndFindElement(WebDriverManager driverManager, String url, By by, int retries) {
    while (true) {
      try {
        final WebDriver driver = driverManager.getDriver();
        driver.navigate().to(url);
        return driver.findElement(by);
      } catch (NoSuchElementException e) {
        if (retries > 0) {
          driverManager.close();
          retries--;
        } else {
          throw e;
        }
      }
    }
  }

  public static Object replaceElement(WebDriver driver, WebElement element, String s) {
    final String script = "var e = arguments[0]; e.outerHTML = arguments[1];";
    return executeScript(driver, script, element, s);
  }

  public static Object setElementDisplay(WebDriver driver, WebElement element, String display) {
    final String script = "var e = arguments[0]; e.style.display = arguments[1];";
    return executeScript(driver, script, element, display);
  }

  private static Object executeScript(WebDriver driver, String script, Object... args) {
    return ((JavascriptExecutor) driver).executeScript(script, args);
  }
}
