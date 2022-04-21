package com.ericrobertbrewer.lectern.app.model;

import com.ericrobertbrewer.lectern.db.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class GeneralConferenceAddress implements DatabaseTable {

  public static final String TABLE_GENERAL_CONFERENCE_ADDRESS = "GeneralConferenceAddress";
  public static final String CREATE_STMT = "CREATE TABLE IF NOT EXISTS " + TABLE_GENERAL_CONFERENCE_ADDRESS + " (" +
    "conference TEXT NOT NULL" + // 2022-04
    ", ordinal INTEGER NOT NULL" + // 0
    ", session TEXT NOT NULL" + // Saturday Morning Session
    ", speaker TEXT DEFAULT NULL" + // Russell M. Nelson
    ", title TEXT NOT NULL" + // Preaching the Gospel of Peace
    ", description TEXT DEFAULT NULL" + // President Nelson teaches that we must stand in holy places...
    ", role TEXT DEFAULT NULL" + // President of the Church of Jesus Christ of Latter-day Saints
    ", kicker TEXT DEFAULT NULL" + // We have the sacred responsibility to share the power and peace of...
    ", url TEXT NOT NULL" + // https://www.churchofjesuschrist.org/study/general-conference/2022/04/11nelson...
    ", filename_html TEXT DEFAULT NULL" + // 11nelson.html
    ", filename_text TEXT DEFAULT NULL" + // 11nelson.txt
    ", category TEXT DEFAULT NULL" + // (instruction|sustaining|auditing|video|...)
    ", PRIMARY KEY (conference, ordinal)" +
    ");";

  private final String conference;
  private final int ordinal;
  private String session;
  private String speaker;
  private String title;
  private String description;
  private String role;
  private String kicker;
  private String url;
  private String filenameHtml;
  private String filenameText;
  private String category;

  public GeneralConferenceAddress(String conference, int ordinal, String session, String title, String url) {
    this.conference = conference;
    this.ordinal = ordinal;
    this.session = session;
    this.title = title;
    this.url = url;
  }

  public String getConference() {
    return conference;
  }

  public int getOrdinal() {
    return ordinal;
  }

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public String getSpeaker() {
    return speaker;
  }

  public void setSpeaker(String speaker) {
    this.speaker = speaker;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getKicker() {
    return kicker;
  }

  public void setKicker(String kicker) {
    this.kicker = kicker;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getFilenameHtml() {
    return filenameHtml;
  }

  public void setFilenameHtml(String filenameHtml) {
    this.filenameHtml = filenameHtml;
  }

  public String getFilenameText() {
    return filenameText;
  }

  public void setFilenameText(String filenameText) {
    this.filenameText = filenameText;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Override
  public int insertOrReplace(Connection connection) throws SQLException {
    final PreparedStatement insert = connection.prepareStatement("INSERT OR REPLACE" +
      " INTO " + TABLE_GENERAL_CONFERENCE_ADDRESS + " (" +
      "conference" +
      ",ordinal" +
      ",session" +
      ",speaker" +
      ",title" +
      ",description" +
      ",role" +
      ",kicker" +
      ",url" +
      ",filename_html" +
      ",filename_text" +
      ",category" +
      ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?);");
    insert.setString(1, getConference());
    insert.setInt(2, getOrdinal());
    insert.setString(3, getSession());
    DatabaseHelper.setStringOrNull(insert, 4, getSpeaker());
    insert.setString(5, getTitle());
    DatabaseHelper.setStringOrNull(insert, 6, getDescription());
    DatabaseHelper.setStringOrNull(insert, 7, getRole());
    DatabaseHelper.setStringOrNull(insert, 8, getKicker());
    insert.setString(9, getUrl());
    DatabaseHelper.setStringOrNull(insert, 10, getFilenameHtml());
    DatabaseHelper.setStringOrNull(insert, 11, getFilenameText());
    DatabaseHelper.setStringOrNull(insert, 12, getCategory());
    final int result = insert.executeUpdate();
    insert.close();
    return result;
  }
}
