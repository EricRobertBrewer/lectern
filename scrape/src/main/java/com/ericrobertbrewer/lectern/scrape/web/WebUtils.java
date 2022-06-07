package com.ericrobertbrewer.lectern.scrape.web;

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

  /**
   * Forbid redirects to A/B testing as {@code https://abn.churchofjesuschrist.org/...}.
   * <p>
   * <a href="https://tech.churchofjesuschrist.org/forum/viewtopic.php?t=35768">Forum topic</a>
   * @param url Church URL.
   * @return URL that disallows A/B testing.
   */
  public static String forbidChurchTesting(String url) {
    if (!url.startsWith("https://www.churchofjesuschrist.org")) {
      throw new IllegalArgumentException("URL isn't in the Church domain: " + url);
    }
    return appendQuery(url, "mboxDisable", "1");
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
    return context.findElement(bys[bys.length - 1]);
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

  private boolean hasTextOutsideOfChildren(WebElement element) {
    // 0: <p>Lorem ipsum.</p>
    // 0: <p><em>Lorem ipsum.</em></p>
    // 0: <p class="big">Lorem ipsum!</p>
    // 1: <p><i>Lorem</i> ipsum.</p>
    // 0: <p><em>Lorem</em> <em>ipsum.</em></p>

    // https://htmlparser.info/parser/
    //  0 plain             { \w: 0, \s: 0,  <: 1 } <- generally, but in this function we { \s: return true; }
    //  1 tag-start         { \w: 1, \s: 2,  /: 9 }
    //  2 tag-o-name        { \s: 2, \w: 3,  /: 8 }
    //  3 tag-o-w           { \w: 3, \s: 4,  /: 8 }
    //  4 tag-o-key         { \w: 3, \s: 4,  =: 5,  /: 8 }
    //  5 tag-o-eq          { \w: 5, \s: 6,  ": 7 }
    //  6 tag-o-val         { \w: 3, \s: 6,  /: 8 }
    //  7 tag-o-val-str     {  ": 3, \w: 7, \s: 7: \\: 8 }
    //  8 tag-o-val-str-esc {
    //  8 tag-o-slash       {  >: 0 }
    //  9 tag-close         { \w: 9, \s: 10 }
    // 10 tag-c-name        {  >: 0, \s: 10, \w: 11 }
    // 11 tag-c-w           {  >: 0, \w: 11 }
    int state = 0;
    final String html = element.getAttribute("innerHTML");
    return false;
  }
}
