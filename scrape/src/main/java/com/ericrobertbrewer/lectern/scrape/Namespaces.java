package com.ericrobertbrewer.lectern.scrape;

import java.io.File;

/**
 * Set of folder names, database tables, etc. which aren't allowed to collide.
 */
public final class Namespaces {

  /**
   * Root of downloaded (scraped) content.
   */
  public static final String FOLDER_APP_ROOT = ".." + File.separator + "app";
  public static final String FOLDER_LOG_ROOT = ".." + File.separator + "log";

  public static final String DATABASE_PATH_APP_DEFAULT = FOLDER_APP_ROOT + File.separator + "app.db";

  public static final String APP_GENERAL_CONFERENCE = "general-conference";
  public static final String TABLE_GENERAL_CONFERENCE = "GeneralConference";
  public static final String TABLE_GENERAL_CONFERENCE_ADDRESS = "GeneralConferenceAddress";
  public static final String TABLE_GENERAL_CONFERENCE_ADDRESS_REF = "GeneralConferenceAddressRef";

  private Namespaces() {
  }
}
