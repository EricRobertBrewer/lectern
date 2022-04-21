package com.ericrobertbrewer.lectern.app;

import com.ericrobertbrewer.lectern.Folders;
import com.ericrobertbrewer.lectern.app.model.GeneralConference;
import com.ericrobertbrewer.lectern.app.model.GeneralConferenceAddress;
import com.ericrobertbrewer.lectern.db.DatabaseHelper;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ScrapeGeneralConferences {

  private static final String APP_NAME = "general-conference";
  private static final String URL_COMPONENT = "general-conference";

  public static void main(String[] args) throws IOException, SQLException {
    final File appFolder = new File(Folders.APP_ROOT + File.separator + APP_NAME);
    if (!appFolder.exists() && !appFolder.mkdirs()) {
      throw new RuntimeException("Unable to create folder `" + appFolder.getPath() + "`.");
    }
    final File logFolder = new File(Folders.LOG_ROOT + File.separator + APP_NAME);
    if (!logFolder.exists() && !logFolder.mkdirs()) {
      throw new RuntimeException("Unable to create folder `" + logFolder.getPath() + "`.");
    }
    final Logger logger = Logger.getLogger(ScrapeGeneralConferences.class.getName());
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.US);
    final String nowFormat = dateTimeFormatter.format(LocalDateTime.now());
    final File logFile = new File(logFolder.getPath() + File.separator + nowFormat);
    final FileHandler logFileHandler = new FileHandler(logFile.getPath());
    final Formatter formatter = new SimpleFormatter();
    logFileHandler.setFormatter(formatter);
    logger.addHandler(logFileHandler);
    final DatabaseHelper databaseHelper = new DatabaseHelper();
    databaseHelper.connect(Folders.APP_DATABASE_DEFAULT);
    databaseHelper.executeSql(GeneralConference.CREATE_STMT);
    databaseHelper.executeSql(GeneralConferenceAddress.CREATE_STMT);
    final WebDriver driver = new ChromeDriver();

    // Collect links to sessions and decades.
    driver.navigate().to("https://www.churchofjesuschrist.org/study/general-conference?lang=eng");
    final List<String> conferenceUrls = new ArrayList<>();
    final List<String> decadeUrls = new ArrayList<>();
    final WebElement mainSection = driver.findElement(By.id("main"));
    final List<WebElement> as = mainSection.findElements(By.tagName("a"));
    for (WebElement a : as) {
      final String url = a.getAttribute("href");
      final List<String> components = Arrays.asList(url.split("/"));
      final int gcIndex = components.indexOf("general-conference");
      if (gcIndex < 0 || gcIndex + 1 >= components.size() ||
        !(components.get(gcIndex + 1).startsWith("2") || components.get(gcIndex + 1).startsWith("1"))) {
        continue; // Skip speakers and topics.
      }
      final String yearComponent = components.get(gcIndex + 1);
      if (yearComponent.length() == 4) {
        logger.info("Found conference: " + url);
        conferenceUrls.add(url);
      } else {
        logger.info("Found decade: " + url);
        decadeUrls.add(url);
      }
    }

    // Collect links to conferences within decades.
    for (String decadeHref : decadeUrls) {
      logger.info("Navigating to decade: " + decadeHref);
      driver.navigate().to(decadeHref);
      final WebElement decadeMainSection = driver.findElement(By.id("main"));
      final List<WebElement> decadeAs = decadeMainSection.findElements(By.tagName("a"));
      for (WebElement decadeA : decadeAs) {
        final String url = decadeA.getAttribute("href");
        logger.info("Found conference: " + url);
        conferenceUrls.add(url);
      }
    }

    // Collect links to addresses.
    for (String conferenceUrl : conferenceUrls) {
      final String conference = getConference(conferenceUrl);
      if (GeneralConference.select(databaseHelper.getConnection(), conference) != null) {
        logger.info("Skipping conference: " + conference);
        continue;
      }
      // Collect metadata.
      logger.info("Navigating to conference: " + conference + ", " + conferenceUrl);
      driver.navigate().to(conferenceUrl);
      final WebElement conferenceMainArticle = driver.findElement(By.id("main"));
      final WebElement manifestNav = conferenceMainArticle.findElement(By.className("manifest"));
      final WebElement sessionUl = manifestNav.findElement(By.tagName("ul"));
      final List<WebElement> sessionLis = sessionUl.findElements(By.xpath("./*"));
      int ordinal = 0;
      for (WebElement sessionLi : sessionLis) {
        final List<WebElement> divUl = sessionLi.findElements(By.xpath("./*"));
        if (divUl.size() != 2) {
          throw new RuntimeException("Expected title <div> and address <ul> in session <li>.");
        }
        final WebElement sessionTitleP = divUl.get(0).findElement(By.tagName("p"));
        final String session = sessionTitleP.getText();
        logger.info("Found session: " + session);
        final WebElement addressUl = divUl.get(1);
        final List<WebElement> addressLis = addressUl.findElements(By.xpath("./*"));
        for (WebElement addressLi : addressLis) {
          final WebElement titleP = addressLi.findElement(By.className("title"));
          final String title = titleP.getText();
          if (title.equals(session)) {
            continue;
          }
          final WebElement a = addressLi.findElement(By.tagName("a"));
          final String url = a.getAttribute("href");
          final GeneralConferenceAddress address = new GeneralConferenceAddress(conference, ordinal, session, title, url);
          try {
            final WebElement primaryMetaP = addressLi.findElement(By.className("primaryMeta"));
            address.setSpeaker(primaryMetaP.getText());
          } catch (NoSuchElementException ignored) {
          }
          try {
            final WebElement descriptionP = addressLi.findElement(By.className("description"));
            address.setDescription(descriptionP.getText());
          } catch (NoSuchElementException ignored) {
          }
          logger.info("Inserting address: " + address.getOrdinal() + ", " + address.getTitle() + ", " + address.getUrl());
          address.insertOrReplace(databaseHelper.getConnection());
          ordinal++;
        }
      }
      final GeneralConference generalConference = new GeneralConference(conference, ordinal, conferenceUrl);
      generalConference.insertOrReplace(databaseHelper.getConnection());
    }

    // Release resources.
    driver.quit();
    databaseHelper.close();
    logFileHandler.close();
  }

  public static String getConference(String url) {
    final String[] urlParams = url.split("\\?");
    final String baseUrl = urlParams[0];
    final List<String> components = Arrays.asList(baseUrl.split("/"));
    final int gcIndex = components.indexOf(URL_COMPONENT);
    if (gcIndex < 0 || gcIndex + 2 >= components.size()) {
      throw new IllegalArgumentException("Unable to get conference from `" + url + "`.");
    }
    return components.get(gcIndex + 1) + "-" + components.get(gcIndex + 2);
  }
}
