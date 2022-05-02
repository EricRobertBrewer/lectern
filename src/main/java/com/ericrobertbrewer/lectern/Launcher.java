package com.ericrobertbrewer.lectern;

import com.ericrobertbrewer.lectern.app.AppScraper;
import com.ericrobertbrewer.lectern.db.DatabaseUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Utility for creating resources for an {@link AppScraper}.
 */
public final class Launcher {

  /**
   * Creates resources for {@link AppScraper#scrape(WebDriver, Connection, File, Logger)}.
   * @param scraper Scraper.
   * @param appName Name of content folder that will be created beneath {@code app/}.
   */
  public static void launch(AppScraper scraper, String appName) {
    // Create web driver.
    final WebDriver driver = createDriver();
    // Prepare content folder.
    final File appFolder = new File(Namespaces.FOLDER_APP_ROOT + File.separator + appName);
    if (!appFolder.exists() && !appFolder.mkdirs()) {
      throw new RuntimeException("Unable to create folder `" + appFolder.getPath() + "`.");
    }
    // Prepare logging.
    final File logFolder = new File(Namespaces.FOLDER_LOG_ROOT + File.separator + appName);
    if (!logFolder.exists() && !logFolder.mkdirs()) {
      throw new RuntimeException("Unable to create folder `" + logFolder.getPath() + "`.");
    }
    final Logger logger = Logger.getLogger(scraper.getClass().getName());
    final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.US);
    final String nowFormat = dateTimeFormatter.format(LocalDateTime.now());
    final File logFile = new File(logFolder.getPath() + File.separator + nowFormat);
    final FileHandler logFileHandler;
    try {
      logFileHandler = new FileHandler(logFile.getPath());
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file handler with path `" + logFile.getPath() + "`.", e);
    }
    System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] %4$s: %2$s %5$s%6$s%n");
    final Formatter formatter = new SimpleFormatter();
    logFileHandler.setFormatter(formatter);
    logger.addHandler(logFileHandler);

    try (Connection connection = DatabaseUtils.connect(Namespaces.DATABASE_PATH_APP_DEFAULT)) {
      scraper.scrape(driver, connection, appFolder, logger);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    } finally {
      logFileHandler.close();
      driver.quit();
    }
  }

  private static WebDriver createDriver() {
    if (System.getProperty("webdriver.chrome.driver") != null) {
      return new ChromeDriver();
    }
    if (System.getProperty("webdriver.gecko.driver") != null) {
      return new FirefoxDriver();
    }
    throw new RuntimeException(
      "No web drivers found. Add one to the VM options as `-Dwebdriver.[chrome|gecko].driver=/path/to/driver`.");
  }

  private Launcher() {
  }
}
