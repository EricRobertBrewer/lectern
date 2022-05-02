package com.ericrobertbrewer.lectern.app;

import com.ericrobertbrewer.lectern.Launcher;
import com.ericrobertbrewer.lectern.Namespaces;
import com.ericrobertbrewer.lectern.app.model.GeneralConferenceAddress;
import com.ericrobertbrewer.lectern.app.model.GeneralConferenceAddressRef;
import com.ericrobertbrewer.lectern.db.DatabaseUtils;
import com.ericrobertbrewer.lectern.text.TextUtils;
import com.ericrobertbrewer.lectern.web.WebUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class GeneralConferenceAddressScraper implements AppScraper {

  public static void main(String[] args) {
    final AppScraper scraper = new GeneralConferenceAddressScraper();
    Launcher.launch(scraper, Namespaces.APP_GENERAL_CONFERENCE);
  }

  @Override
  public void scrape(WebDriver driver, Connection connection, File appFolder, Logger logger) throws SQLException {
    DatabaseUtils.executeSql(connection, GeneralConferenceAddressRef.CREATE_STMT);

    final List<GeneralConferenceAddress> addresses = GeneralConferenceAddress.selectAll(connection);
    for (GeneralConferenceAddress address : addresses) {
      // Skip processed addresses.
      final File conferenceFolder = new File(appFolder, address.getConference());
      if (!conferenceFolder.exists() && !conferenceFolder.mkdirs()) {
        throw new RuntimeException("Unable to create conference folder: " + address.getConference());
      }
      final File textFile = new File(conferenceFolder, address.getFilename() + ".txt");
      if (textFile.exists()) {
        // TODO: Add `force` flag to overwrite.
        continue;
      }

      logger.info("Scraping address: " + address.getPrimaryKey() + " " + address.getUrl());
      driver.navigate().to(address.getUrl());
      final WebElement appDiv = driver.findElement(By.id("app"));
      final WebElement contentSection = appDiv.findElement(By.id("content"));
      final WebElement mainArticle = contentSection.findElement(By.id("main"));
      final WebElement bodyDiv = mainArticle.findElement(By.className("body"));

      // Keep track of references.
      final Map<String, List<Integer>> refsToLines = new HashMap<>();
      final Map<Integer, Integer> notesToLine = new HashMap<>();

      // Find role and kicker.
      boolean updated = false;
      final WebElement header = bodyDiv.findElement(By.tagName("header"));
      try {
        final WebElement bylineDiv = header.findElement(By.className("byline"));
        final By[] bys = {By.id("author2"), By.className("author-role")};
        final WebElement authorP = WebUtils.findElement(bylineDiv, bys);
        address.setRole(authorP.getText());
        updated = true;
      } catch (NoSuchElementException e) {
        try {
          final WebElement bylineDiv = header.findElement(By.className("byline"));
          final List<WebElement> ps = bylineDiv.findElements(By.tagName("p"));
          if (ps.size() == 2) {
            address.setRole(ps.get(1).getText());
            updated = true;
          } else if (ps.size() == 1 && ps.get(0).getText().startsWith("By President")) {
            address.setRole("^By President");
            updated = true;
          } else {
            logger.warning("Unable to find role: " + address.getPrimaryKey());
          }
        } catch (NoSuchElementException ignored) {
        }
      }
      try {
        final By[] bys = {By.id("kicker1"), By.className("kicker")};
        final WebElement kickerP = WebUtils.findElement(header, bys);
        // Yes, a reference can appear in the kicker and only in the kicker.
        // https://www.churchofjesuschrist.org/study/general-conference/2007/10/o-remember-remember?lang=eng
        final int escapedRefs =
          escapeReferences(driver, kickerP, refsToLines, notesToLine, GeneralConferenceAddressRef.LINE_KICKER);
        final String text = TextUtils.removeEscapedReferenceMarkers(kickerP.getText().trim(), escapedRefs);
        address.setKicker(text);
        updated = true;
      } catch (NoSuchElementException ignored) {
      }

      // Collect text and inline, i.e., scripture, references.
      final List<String> textLines = new ArrayList<>();
      final WebElement bodyBlockDiv = WebUtils.findElementOrNull(bodyDiv, By.className("body-block"));
      // The video below has no body, but a blank text file can be made to mark that it has been processed.
      // (Later videos include a transcript.)
      // https://www.churchofjesuschrist.org/study/general-conference/2020/10/33video?lang=eng
      if (bodyBlockDiv != null) {
        appendTextLines(driver, bodyBlockDiv, textLines, refsToLines, notesToLine, logger);
      }

      // Collect references from notes footer.
      final Map<String, List<String>> refsToTexts = new HashMap<>();
      // Most sustainings, audit reports, and statistical reports don't have any notes.
      final WebElement notesFooter = WebUtils.findElementOrNull(bodyDiv, By.tagName("footer"));
      if (notesFooter != null) {
        WebUtils.setElementDisplay(driver, notesFooter, "block");
        final WebElement notesOl = notesFooter.findElement(By.tagName("ol"));
        final List<WebElement> notesLis = notesOl.findElements(By.tagName("li"));
        for (WebElement notesLi : notesLis) {
          // Don't rely on the `id` attribute of the <li> as "note\d+" because it can be incorrect!
          // https://www.churchofjesuschrist.org/study/general-conference/2020/04/13rasband?lang=eng
          final String dataMarker = notesLi.getAttribute("data-marker"); // Assumes "\d+\.".
          final int note = Integer.parseInt(dataMarker.substring(0, dataMarker.length() - 1));
          final int line;
          if (notesToLine.containsKey(note)) {
            line = notesToLine.get(note);
          } else if (note == notesToLine.size() + 1) {
            // Some addresses have one extraneous reference that isn't linked in the text body.
            // https://www.churchofjesuschrist.org/study/general-conference/2006/10/the-gathering-of-scattered-israel?lang=eng
            // Refer to the last line.
            line = textLines.size() - 1;
          } else {
            throw new NullPointerException("Note " + note + " is not linked in address " + address.getPrimaryKey());
          }
          final List<WebElement> ps = notesLi.findElements(By.tagName("p"));
          for (WebElement p : ps) {
            final List<WebElement> as = p.findElements(By.tagName("a"));
            for (WebElement a : as) {
              final String ref = a.getAttribute("href").split("#")[0];
              if (!refsToLines.containsKey(ref)) {
                refsToLines.put(ref, new ArrayList<>());
              }
              refsToLines.get(ref).add(line);
              if (!refsToTexts.containsKey(ref)) {
                refsToTexts.put(ref, new ArrayList<>());
              }
              refsToTexts.get(ref).add(p.getText());
            }
          }
        }
      }

      // Persist references.
      for (String ref : refsToLines.keySet()) {
        final int[] lines = refsToLines.get(ref).stream().mapToInt(v -> v).toArray();
        final String[] texts;
        if (refsToTexts.containsKey(ref)) {
          texts = refsToTexts.get(ref).toArray(new String[0]);
        } else {
          texts = null;
        }
        final GeneralConferenceAddressRef addressRef =
          new GeneralConferenceAddressRef(address.getConference(), address.getOrdinal(), ref, lines, texts);
        addressRef.insertOrReplace(connection);
      }

      // Update role and kicker.
      if (updated) {
        address.insertOrReplace(connection);
        logger.info("Updated address: " + address.getPrimaryKey());
      }

      // Write text file.
      try {
        TextUtils.writeLines(textLines, textFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static int escapeReferences(
    WebDriver driver,
    WebElement element,
    Map<String, List<Integer>> refsToLines,
    Map<Integer, Integer> notesToLine,
    int line
  ) {
    int escapedRefs = 0;
    final List<WebElement> as = element.findElements(By.tagName("a"));
    for (WebElement a : as) {
      final String aText = a.getText();
      final String className = a.getAttribute("className");
      final String ref = a.getAttribute("href").split("#")[0];
      if ("note-ref".equals(className)) {
        // Remove note text.
        final int note = Integer.parseInt(aText);
        notesToLine.put(note, line);
        WebUtils.replaceElement(driver, a, "");
      } else if (
        "scripture-ref".equals(className) ||
          "cross-ref".equals(className) ||
          // https://www.churchofjesuschrist.org/study/general-conference/2021/04/33nielsen?lang=eng
          "[object Object]".equals(a.getAttribute("to")) ||
          // https://www.churchofjesuschrist.org/study/general-conference/2022/04/24kearon?lang=eng
          ref.startsWith("https://www.churchofjesuschrist.org/")
      ) {
        // Escape reference.
        if (!refsToLines.containsKey(ref)) {
          refsToLines.put(ref, new ArrayList<>());
        }
        refsToLines.get(ref).add(line);
        WebUtils.replaceElement(driver, a, TextUtils.REF_START + aText + TextUtils.REF_END);
        escapedRefs++;
      } else {
        throw new RuntimeException("Unknown <a> class name: " + className);
      }
    }
    return escapedRefs;
  }

  private static void appendTextLines(
    WebDriver driver,
    WebElement element,
    List<String> lines,
    Map<String, List<Integer>> refsToLines,
    Map<Integer, Integer> notesToLine,
    Logger logger
  ) {
    // TODO: `boolean doesHaveVisibleTextOutsideOfChildren(WebElement element) {}`
    final List<WebElement> children = element.findElements(By.xpath("./*"));
    if (children.size() == 0) {
      // Get visible text.
      final String text = element.getText().trim();
      if (text.length() > 0) {
        lines.add(text);
      }
    } else if (children.stream().map(WebElement::getTagName).allMatch(FORMATTING_TAGS::contains)) {
      // Collapse and escape <a> contents; ignore effects of others.
      final int escapedRefs = escapeReferences(driver, element, refsToLines, notesToLine, lines.size());
      final String text = TextUtils.removeEscapedReferenceMarkers(element.getText().trim(), escapedRefs);
      if (text.length() > 0) {
        lines.add(text);
      }
    } else {
      // Process each child individually instead.
      for (WebElement child : children) {
        final String tag = child.getTagName();
        if (IGNORE_TAGS.contains(tag)) {
          continue;
        }
        if (CONTENT_TAGS.contains(tag) || FORMATTING_TAGS.contains(tag)) {
          appendTextLines(driver, child, lines, refsToLines, notesToLine, logger);
          continue;
        }
        throw new RuntimeException("Unknown tag in " + element.getAttribute("outerHTML") + ".");
      }
    }
  }

  private static final Set<String> FORMATTING_TAGS = new HashSet<>();
  static {
    FORMATTING_TAGS.addAll(Arrays.asList("b", "i", "strong", "em", "a", "sup", "cite", "span"));
    // <span> page break: https://www.churchofjesuschrist.org/study/general-conference/2021/10/11nelson?lang=eng
  }

  private static final Set<String> IGNORE_TAGS = new HashSet<>();
  static {
    IGNORE_TAGS.addAll(Arrays.asList(
      "img", "video", "noscript",
      // Even though figures have a caption, they're short and not part of the monologue.
      // https://www.churchofjesuschrist.org/study/general-conference/2014/10/joseph-smith?lang=eng
      "figure",
      // Inline videos come with controls.
      // https://www.churchofjesuschrist.org/study/general-conference/2021/10/47nelson?lang=eng
      "button", "svg", "label", "select"));
  }

  private static final Set<String> CONTENT_TAGS = new HashSet<>();
  static {
    CONTENT_TAGS.addAll(Arrays.asList(
      "p",
      // https://www.churchofjesuschrist.org/study/general-conference/2017/10/exceeding-great-and-precious-promises?lang=eng
      "h2", "h3", "header",
      // https://www.churchofjesuschrist.org/study/general-conference/2018/04/teaching-in-the-home-a-joyful-and-sacred-responsibility?lang=eng
      "section",
      "ul", "ol", "li",
      // Tables can be included in statistical reports.
      // https://www.churchofjesuschrist.org/study/general-conference/2017/04/statistical-report-2016?lang=eng
      "table", "tbody", "tr", "td",
      // Sometimes, a <div> contains indented poetry.
      // https://www.churchofjesuschrist.org/study/general-conference/2018/04/small-and-simple-things?lang=eng
      "div",
      // Some talks have an annotation within a card.
      // https://www.churchofjesuschrist.org/study/general-conference/2012/10/i-know-it-i-live-it-i-love-it?lang=eng
      "aside"));
  }
}
