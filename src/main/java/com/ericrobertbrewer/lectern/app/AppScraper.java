package com.ericrobertbrewer.lectern.app;

import com.ericrobertbrewer.lectern.Launcher;
import com.ericrobertbrewer.lectern.web.WebDriverManager;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * A class which scrapes from a particular domain of web pages.
 */
public interface AppScraper {

  /**
   * Scrape a set of web pages on a single thread. Called from {@link Launcher#launch(AppScraper, String)}.
   *
   * @param driverManager Web driver manager.
   * @param connection Connection to the shared app database.
   * @param appFolder Folder to which any scraped files will be stored.
   * @param logger Logger.
   */
  void scrape(WebDriverManager driverManager, Connection connection, File appFolder, Logger logger) throws SQLException;
}
