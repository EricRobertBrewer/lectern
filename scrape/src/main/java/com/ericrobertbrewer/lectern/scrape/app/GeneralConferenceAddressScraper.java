package com.ericrobertbrewer.lectern.scrape.app;

import com.ericrobertbrewer.lectern.scrape.Launcher;
import com.ericrobertbrewer.lectern.scrape.Namespaces;
import com.ericrobertbrewer.lectern.scrape.app.model.GeneralConferenceAddress;
import com.ericrobertbrewer.lectern.scrape.app.model.GeneralConferenceAddressRef;
import com.ericrobertbrewer.lectern.scrape.db.DatabaseUtils;
import com.ericrobertbrewer.lectern.scrape.text.TextUtils;
import com.ericrobertbrewer.lectern.scrape.web.WebDriverManager;
import com.ericrobertbrewer.lectern.scrape.web.WebUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
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
  public void scrape(
    WebDriverManager driverManager,
    Connection connection,
    File appFolder,
    Logger logger
  ) throws SQLException {
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
      // Forbid redirects to A/B testing as `https://abn.churchofjesuschrist.org/...`.
      final String urlNoAbn = WebUtils.appendQuery(address.getUrl(), "mboxDisable", "1");
      final By appBy = By.id("app");
      final int retries = 2;
      final WebElement appDiv;
      try {
        appDiv = WebUtils.navigateAndFindElement(driverManager, urlNoAbn, appBy, retries);
      } catch (NoSuchElementException e) {
        throw new RuntimeException("Unable to find element " + appBy + " after " + retries + " retries.", e);
      }
      final WebElement contentSection = appDiv.findElement(By.id("content"));
      final WebElement mainArticle = contentSection.findElement(By.id("main"));
      final WebElement bodyDiv = mainArticle.findElement(By.className("body"));

      // Keep track of references.
      final Map<String, List<Integer>> refsToLines = new HashMap<>();
      // Unfortunately, a note can be an asterisk...
      // https://www.churchofjesuschrist.org/study/general-conference/1990/10/covenants
      final Map<String, Integer> notesToLine = new HashMap<>();

      // Rarely, a note can be in the title.
      // https://www.churchofjesuschrist.org/study/general-conference/1975/10/the-welfare-production-distribution-department?lang=eng
      final WebElement header = bodyDiv.findElement(By.tagName("header"));
      final WebElement titleH1 = WebUtils.findElement(header, new By[]{By.id("title1"), By.tagName("h1")});
      escapeReferences(driverManager.getDriver(), titleH1, refsToLines, notesToLine, GeneralConferenceAddressRef.LINE_TITLE);

      // Find speaker, role, and/or kicker.
      boolean updated = false;
      if (address.getSpeaker() == null) {
        try {
          final WebElement bylineDiv = header.findElement(By.className("byline"));
          // https://www.churchofjesuschrist.org/study/general-conference/1974/04/church-finance-committee-report?lang=eng
          final By[] bys = {By.id("author1"), By.className("author-name")};
          final WebElement authorNameP = WebUtils.findElement(bylineDiv, bys);
          final String text = authorNameP.getText();
          final String[] prefixes = {"By ", "Presented by "};
          for (String prefix : prefixes) {
            if (text.startsWith(prefix)) {
              address.setSpeaker(text.substring(prefix.length()));
              updated = true;
              break;
            }
          }
        } catch (NoSuchElementException e) {
          try {
            final WebElement bodyBlockDiv = bodyDiv.findElement(By.className("body-block"));
            // https://www.churchofjesuschrist.org/study/general-conference/1971/10/sustaining-of-general-authorities-and-officers?lang=eng
            final WebElement p = bodyBlockDiv.findElement(By.tagName("p"));
            final WebElement strong = p.findElement(By.tagName("strong"));
            final String text = strong.getText();
            final String prefix = "President ";
            if (text.startsWith(prefix)) {
              if (text.endsWith(":")) {
                // https://www.churchofjesuschrist.org/study/general-conference/1974/10/sustaining-of-church-officers?lang=eng
                address.setSpeaker(text.substring(prefix.length(), text.length() - 1));
              } else {
                address.setSpeaker(text.substring(prefix.length()));
              }
              updated = true;
            }
          } catch (NoSuchElementException e2) {
            try {
              final WebElement bodyBlockDiv = bodyDiv.findElement(By.className("body-block"));
              // https://www.churchofjesuschrist.org/study/general-conference/1975/04/church-finance-committee-report?lang=eng
              final WebElement closingBlockDiv = bodyBlockDiv.findElement(By.className("closing-block"));
              final List<WebElement> ps = closingBlockDiv.findElements(By.tagName("p"));
              for (int i = 0; i < ps.size() - 1; i++) {
                if ("CHURCH FINANCE COMMITTEE".equals(ps.get(i).getText())) {
                  address.setSpeaker(ps.get(i + 1).getText());
                  updated = true;
                  break;
                }
              }
            } catch (NoSuchElementException ignored) {
            }
          }
        }
      }
      try {
        final WebElement bylineDiv = header.findElement(By.className("byline"));
        final By[] bys = {By.id("author2"), By.className("author-role")};
        final WebElement authorRoleP = WebUtils.findElement(bylineDiv, bys);
        address.setRole(authorRoleP.getText());
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
        final int escapedRefs = escapeReferences(
          driverManager.getDriver(),
          kickerP,
          refsToLines,
          notesToLine,
          GeneralConferenceAddressRef.LINE_KICKER);
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
        appendTextLines(driverManager.getDriver(), bodyBlockDiv, textLines, refsToLines, notesToLine, logger);
      }

      // Collect references from notes footer.
      final Map<String, List<String>> refsToTexts = new HashMap<>();
      // Most sustainings, audit reports, and statistical reports don't have any notes.
      final WebElement notesFooter = WebUtils.findElementOrNull(bodyDiv, By.tagName("footer"));
      if (notesFooter != null) {
        WebUtils.setElementDisplay(driverManager.getDriver(), notesFooter, "block");
        // Almost always a <ol>, but can be a <ul class="symbol">.
        // https://www.churchofjesuschrist.org/study/general-conference/1990/10/covenants
        final By[] bys = {By.tagName("ol"), By.tagName("ul")};
        final WebElement notesOl = WebUtils.findElement(notesFooter, bys);
        final List<WebElement> notesLis = notesOl.findElements(By.tagName("li"));
        for (WebElement notesLi : notesLis) {
          // Don't rely on the `id` attribute of the <li> as "note\d+" because it can be incorrect!
          // https://www.churchofjesuschrist.org/study/general-conference/2020/04/13rasband?lang=eng
          final String dataMarker = notesLi.getAttribute("data-marker");
          final String note;
          if (dataMarker.endsWith(".")) {
            // As "\d+\.", e.g., "12.".
            note = dataMarker.substring(0, dataMarker.length() - 1);
          } else {
            // No trailing period for list items in <ul class="symbol">.
            note = dataMarker;
          }
          final int line;
          if (notesToLine.containsKey(note)) {
            line = notesToLine.get(note);
          } else if (Integer.parseInt(note) == notesToLine.size() + 1) {
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

      // Update speaker, role, and/or kicker.
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
    Map<String, Integer> notesToLine,
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
        notesToLine.put(aText, line);
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
    Map<String, Integer> notesToLine,
    Logger logger
  ) {
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
    FORMATTING_TAGS.addAll(Arrays.asList("b", "i", "strong", "em", "a", "sup", "cite", "span", "sub", "u"));
    // <span> page break: https://www.churchofjesuschrist.org/study/general-conference/2021/10/11nelson?lang=eng
    // <sub> (H_2O) https://www.churchofjesuschrist.org/study/general-conference/1984/04/the-simplicity-of-gospel-truths
    // <u> https://www.churchofjesuschrist.org/study/general-conference/1974/04/planning-for-a-full-and-abundant-life?lang=eng
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
      "button", "svg", "label", "select",
      // https://www.churchofjesuschrist.org/study/general-conference/1982/10/run-boy-run?lang=eng
      "br"));
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
      "aside",
      // https://www.churchofjesuschrist.org/study/general-conference/1983/04/anonymous?lang=eng
      "blockquote"));
  }
}
