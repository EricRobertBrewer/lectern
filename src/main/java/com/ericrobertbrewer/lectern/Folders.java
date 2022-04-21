package com.ericrobertbrewer.lectern;


import java.io.File;

public final class Folders {

  /**
   * Root of downloaded (scraped) content.
   */
  public static final String APP_ROOT = "app";
  public static final String APP_DATABASE_DEFAULT = APP_ROOT + File.separator + "app.db";
  public static final String LOG_ROOT = "log";

  private Folders() {
  }
}
