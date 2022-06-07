package com.ericrobertbrewer.lectern.scrape.app;

import com.ericrobertbrewer.lectern.scrape.Launcher;
import com.ericrobertbrewer.lectern.scrape.Namespaces;
import com.ericrobertbrewer.lectern.scrape.app.model.ScriptureChapter;
import com.ericrobertbrewer.lectern.scrape.app.model.ScriptureChapterRef;
import com.ericrobertbrewer.lectern.scrape.app.model.ScriptureInfo;
import com.ericrobertbrewer.lectern.scrape.db.DatabaseUtils;
import com.ericrobertbrewer.lectern.scrape.text.TextUtils;
import com.ericrobertbrewer.lectern.scrape.web.WebDriverManager;
import com.ericrobertbrewer.lectern.scrape.web.WebUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class ScriptureChapterScraper implements AppScraper {

  public static void main(String[] args) {
    final AppScraper scraper = new ScriptureChapterScraper();
    Launcher.launch(scraper, Namespaces.APP_SCRIPTURES);
  }

  @Override
  public void scrape(WebDriverManager driverManager, Connection connection, File appFolder, Logger logger)
      throws SQLException {
    DatabaseUtils.executeSql(connection, ScriptureChapter.CREATE_STMT);
    DatabaseUtils.executeSql(connection, ScriptureChapterRef.CREATE_STMT);

    // Collect chapters of books.
    final String scripturesUrl = "https://www.churchofjesuschrist.org/study/scriptures";
    final List<String> chapterUrls = new ArrayList<>();
    for (String testament : ScriptureInfo.TESTAMENTS) {
      for (String book : ScriptureInfo.TESTAMENT_BOOKS.get(testament)) {
        if (ScriptureInfo.BOOKS_TEXT_CONTENT.contains(book)) {
          chapterUrls.add(scripturesUrl + String.format("/%s/%s?lang=eng", testament, book));
        } else if (ScriptureInfo.BOOKS_ONE_CHAPTER.contains(book)) {
          chapterUrls.add(scripturesUrl + String.format("/%s/%s/1?lang=eng", testament, book));
        } else {
          // Collect chapters of this multi-chapter book.
          final String bookUrl = scripturesUrl + String.format("/%s/%s?lang=eng", testament, book);
          logger.info("Collecting chapters for book: " + bookUrl);
          final WebElement appDiv =
              driverManager.navigateAndFindElement(WebUtils.forbidChurchTesting(bookUrl), By.id("app"), 2);
          final WebElement contentSection = appDiv.findElement(By.id("content"));
          final WebElement mainArticle = contentSection.findElement(By.id("main"));
          final WebElement bodyDiv = mainArticle.findElement(By.className("body"));
          final WebElement tocNav = bodyDiv.findElement(By.className("toc"));
          final WebElement ul = tocNav.findElement(By.tagName("ul"));
          final List<WebElement> lis = ul.findElements(By.tagName("li"));
          for (WebElement li : lis) {
            final WebElement listTileA = li.findElement(By.tagName("a"));
            final String url = listTileA.getAttribute("href");
            chapterUrls.add(url);
          }
        }
      }
    }

    for (String chapterUrl : chapterUrls) {
      final ScriptureChapter scriptureChapter = getScriptureChapter(chapterUrl);
      final File chapterFile = scriptureChapter.getChapterFile(appFolder);
      if (chapterFile.exists()) {
        // TODO: Include `force` flag to overwrite.
        continue;
      }

      // Find 'body' element.
      logger.info("Navigating to chapter: " + chapterUrl);
      final WebElement appDiv =
          driverManager.navigateAndFindElement(WebUtils.forbidChurchTesting(chapterUrl), By.id("app"), 2);
      final WebElement contentSection = appDiv.findElement(By.id("content"));
      final WebElement mainArticle = contentSection.findElement(By.id("main"));
      final WebElement bodyDiv = mainArticle.findElement(By.className("body"));

      // Collect lines and references.
      final List<String> chapterLines = new ArrayList<>();
      final Map<String, List<Integer>> refsToLines = new HashMap<>();
      final Map<String, Integer> notesToLine = new HashMap<>();

      // Scrape header.
      final WebElement header = bodyDiv.findElement(By.tagName("header"));
      final List<WebElement> headerPs = header.findElements(By.tagName("p"));
      for (WebElement headerP : headerPs) {
        final String pClassName = headerP.getAttribute("className");
        if (HEADER_P_CLASS_IGNORE.contains(pClassName)) {
          continue;
        }
        final List<WebElement> as = headerP.findElements(By.tagName("a"));
        for (WebElement a : as) {
          final String aClassName = a.getAttribute("className");
          final String ref = a.getAttribute("href");
          if ("scripture-ref".equals(aClassName)) {
            if ("study-summary".equals(pClassName) || "study-intro".equals(pClassName)) {
              if (!refsToLines.containsKey(ref)) {
                refsToLines.put(ref, new ArrayList<>());
              }
              refsToLines.get(ref).add(chapterLines.size());
            } else {
              throw new RuntimeException("Unknown <p> class name: " + pClassName);
            }
          } else {
            throw new RuntimeException("Unknown <a> class name: " + aClassName);
          }
        }
        final String text = headerP.getText();
        chapterLines.add(text);
      }

      // Scrape body block.
      final WebElement bodyBlockDiv = bodyDiv.findElement(By.className("body-block"));
      final List<WebElement> bodyBlockPs = bodyBlockDiv.findElements(By.tagName("p"));
      for (WebElement bodyBlockP : bodyBlockPs) {
        final List<WebElement> as = bodyBlockP.findElements(By.tagName("a"));
        for (WebElement a : as) {
          final String aClassName = a.getAttribute("className");
          if ("study-note-ref".equals(aClassName)) {
            // Keep track of this note.
            final String dataScrollId = a.getAttribute("data-scroll-id");
            notesToLine.put(dataScrollId, chapterLines.size());
            // Remove the superscript text.
            final WebElement sup = WebUtils.findElementOrNull(a, By.tagName("sup"));
            if (sup != null) {
              WebUtils.replaceElement(driverManager.getDriver(), sup, "");
            }
          } else if ("scripture-ref".equals(aClassName)) {
            final String ref = a.getAttribute("href");
            if (!refsToLines.containsKey(ref)) {
              refsToLines.put(ref, new ArrayList<>());
            }
            refsToLines.get(ref).add(chapterLines.size());
          } else {
            throw new RuntimeException("Unknown <a> class name: " + aClassName);
          }
        }
        final String text = bodyBlockP.getText();
        chapterLines.add(text);
      }

      // Process notes.
      final WebElement footer = WebUtils.findElementOrNull(bodyDiv, By.tagName("footer"));
      if (footer != null) {
        // Un-hide.
        WebUtils.setElementDisplay(driverManager.getDriver(), footer, "block");

        final List<WebElement> footerUls = footer.findElements(By.xpath("./ul"));
        for (WebElement footerUl : footerUls) {
          final String footerUlDataType = footerUl.getAttribute("data-type");
          if ("no-marker".equals(footerUlDataType)) {
            // https://www.churchofjesuschrist.org/study/scriptures/ot/song/1?lang=eng
            continue;
          }
          final String footerUlClassName = footerUl.getAttribute("className");
          if ("symbol".equals(footerUlClassName)) {
            // https://www.churchofjesuschrist.org/study/scriptures/ot/mal/4?lang=eng
            continue;
          }
          if (!"chapter".equals(footerUlDataType)) {
            throw new RuntimeException("Expected chapter data type.");
          }
          final WebElement chapterLi = footerUl.findElement(By.tagName("li"));
          final WebElement verseUl = chapterLi.findElement(By.tagName("ul"));
          final String verseUlDataType = verseUl.getAttribute("data-type");
          if (!"verse".equals(verseUlDataType)) {
            throw new RuntimeException("Expected verse data type.");
          }
          final List<WebElement> verseLis = verseUl.findElements(By.xpath("./li"));
          for (WebElement verseLi : verseLis) {
            final WebElement markerUl = verseLi.findElement(By.tagName("ul"));
            final String markerUlDataType = markerUl.getAttribute("data-type");
            if (!"marker".equals(markerUlDataType)) {
              throw new RuntimeException("Expected marker data type.");
            }
            final List<WebElement> markerLis = markerUl.findElements(By.xpath("./li"));
            for (WebElement markerLi : markerLis) {
              final String noteId = markerLi.getAttribute("id");
              final List<WebElement> markerAs = markerLi.findElements(By.tagName("a"));
              for (WebElement markerA : markerAs) {
                final String markerAClassName = markerA.getAttribute("className");
                if ("scripture-ref".equals(markerAClassName)) {
                  final String ref = markerA.getAttribute("href");
                  final int line = notesToLine.get(noteId);
                  if (!refsToLines.containsKey(ref)) {
                    refsToLines.put(ref, new ArrayList<>());
                  }
                  refsToLines.get(ref).add(line);
                } else {
                  throw new RuntimeException("Unknown <a> class name: " + markerAClassName);
                }
              }
            }
          }
        }
      }

      // Persist references.
      for (String ref : refsToLines.keySet()) {
        final String path = scriptureChapter.getPath();
        final int[] lines = refsToLines.get(ref).stream().mapToInt(v -> v).toArray();
        final ScriptureChapterRef scriptureChapterRef = new ScriptureChapterRef(path, lines, ref);
        scriptureChapterRef.insertOrReplace(connection);
      }

      // Persist chapter.
      scriptureChapter.insertOrReplace(connection);

      // Write chapter text.
      try {
        TextUtils.writeToFile(chapterLines, chapterFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final Set<String> HEADER_P_CLASS_IGNORE = new HashSet<>();

  static {
    HEADER_P_CLASS_IGNORE.add("title-number");
  }

  private static ScriptureChapter getScriptureChapter(String chapterUrl) {
    final String[] chapterUrlParams = chapterUrl.split("\\?");
    final String baseChapterUrl = chapterUrlParams[0];
    final List<String> components = Arrays.asList(baseChapterUrl.split("/"));
    final int scripturesIndex = components.indexOf("scriptures");
    final String testament = components.get(scripturesIndex + 1);
    final String book = components.get(scripturesIndex + 2);
    final String chapter;
    final String path;
    if (scripturesIndex + 3 < components.size()) {
      chapter = components.get(scripturesIndex + 3);
      path = String.join(ScriptureChapter.PATH_DELIMITER, testament, book, chapter);
    } else {
      chapter = null;
      path = String.join(ScriptureChapter.PATH_DELIMITER, testament, book);
    }
    return new ScriptureChapter(path, testament, book, chapter);
  }
}
