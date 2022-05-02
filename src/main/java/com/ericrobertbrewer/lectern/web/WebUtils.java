package com.ericrobertbrewer.lectern.web;

import org.openqa.selenium.*;

public final class WebUtils {

  private WebUtils() {
  }

  public static WebElement findElementOrNull(SearchContext context, By by) {
    try {
      return context.findElement(by);
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  public static WebElement findElement(SearchContext context, By[] bys) throws NoSuchElementException {
    for (int i = 0; i < bys.length - 1; i++) {
      try {
        return context.findElement(bys[i]);
      } catch (NoSuchElementException ignored) {
      }
    }
    return context.findElement(bys[bys.length-1]);
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
