package com.ericrobertbrewer.lectern.scrape.app;

import com.ericrobertbrewer.lectern.scrape.Launcher;
import com.ericrobertbrewer.lectern.scrape.Namespaces;
import com.ericrobertbrewer.lectern.scrape.app.model.GeneralConference;
import com.ericrobertbrewer.lectern.scrape.app.model.GeneralConferenceAddress;
import com.ericrobertbrewer.lectern.scrape.db.DatabaseUtils;
import com.ericrobertbrewer.lectern.scrape.web.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class GeneralConferenceScraper implements AppScraper {

  public static final String URL_COMPONENT = "general-conference";

  public static void main(String[] args) {
    final AppScraper scraper = new GeneralConferenceScraper();
    Launcher.launch(scraper, Namespaces.APP_GENERAL_CONFERENCE);
  }

  @Override
  public void scrape(
    WebDriverManager driverManager,
    Connection connection,
    File appFolder,
    Logger logger
  ) throws SQLException {
    DatabaseUtils.executeSql(connection, GeneralConference.CREATE_STMT);
    DatabaseUtils.executeSql(connection, GeneralConferenceAddress.CREATE_STMT);

    // Collect links to sessions and decades.
    driverManager.getDriver().navigate()
      .to("https://www.churchofjesuschrist.org/study/general-conference?lang=eng");
    final List<String> conferenceUrls = new ArrayList<>();
    final List<String> decadeUrls = new ArrayList<>();
    final WebElement mainSection = driverManager.getDriver().findElement(By.id("main"));
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
      driverManager.getDriver().navigate().to(decadeHref);
      final WebElement decadeMainSection = driverManager.getDriver().findElement(By.id("main"));
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
      if (GeneralConference.select(connection, conference) != null) {
        logger.info("Skipping conference: " + conference);
        // TODO: Add `force` flag to overwrite.
        continue;
      }
      // Collect metadata.
      logger.info("Navigating to conference: " + conference + ", " + conferenceUrl);
      driverManager.getDriver().navigate().to(conferenceUrl);
      final WebElement conferenceMainArticle = driverManager.getDriver().findElement(By.id("main"));
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
            // Skip session items.
            continue;
          }
          final WebElement a = addressLi.findElement(By.tagName("a"));
          final String url = a.getAttribute("href");
          final String filename = getFilename(url);
          final GeneralConferenceAddress address = new GeneralConferenceAddress(conference, ordinal, session, title, url, filename);
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
          logger.info("Inserting address: " + address.getPrimaryKey());
          address.insertOrReplace(connection);
          ordinal++;
        }
      }
      final GeneralConference generalConference = new GeneralConference(conference, ordinal, conferenceUrl);
      generalConference.insertOrReplace(connection);
    }
  }

  private static String getConference(String url) {
    final String[] urlParams = url.split("\\?");
    final String baseUrl = urlParams[0];
    final List<String> components = Arrays.asList(baseUrl.split("/"));
    final int gcIndex = components.indexOf(URL_COMPONENT);
    if (gcIndex < 0 || gcIndex + 2 >= components.size()) {
      throw new IllegalArgumentException("Unable to get conference from `" + url + "`.");
    }
    return components.get(gcIndex + 1) + "-" + components.get(gcIndex + 2);
  }

  private static String getFilename(String url) {
    final String[] urlParams = url.split("\\?");
    final String baseUrl = urlParams[0];
    final String[] components = baseUrl.split("/");
    return components[components.length - 1];
  }
}
