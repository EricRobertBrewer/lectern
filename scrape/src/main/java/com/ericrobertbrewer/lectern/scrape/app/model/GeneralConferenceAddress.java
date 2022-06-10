package com.ericrobertbrewer.lectern.scrape.app.model;

import com.ericrobertbrewer.lectern.common.Namespaces;
import com.ericrobertbrewer.lectern.common.db.DatabaseUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GeneralConferenceAddress implements DatabaseTable {

  public static final String CREATE_STMT =
      "CREATE TABLE IF NOT EXISTS " + Namespaces.TABLE_GENERAL_CONFERENCE_ADDRESS + " (" +
          "conference TEXT NOT NULL" + // 2022-04
          ", ordinal INTEGER NOT NULL" + // 0
          ", session TEXT NOT NULL" + // Saturday Morning Session
          ", speaker TEXT DEFAULT NULL" + // Russell M. Nelson
          ", title TEXT NOT NULL" + // Preaching the Gospel of Peace
          ", description TEXT DEFAULT NULL" + // President Nelson teaches that we must stand in holy places...
          ", role TEXT DEFAULT NULL" + // President of the Church of Jesus Christ of Latter-day Saints
          ", kicker TEXT DEFAULT NULL" + // We have the sacred responsibility to share the power and peace of...
          ", url TEXT NOT NULL" + // https://www.churchofjesuschrist.org/study/general-conference/2022/04/11nelson...
          ", filename TEXT NOT NULL" + // 11nelson
          ", category TEXT DEFAULT NULL" + // (instruction|sustaining|auditing|video|...)
          ", PRIMARY KEY (conference, ordinal)" +
          ");";

  private final String conference;
  private final int ordinal;
  private final String session;
  private String speaker;
  private final String title;
  private String description;
  private String role;
  private String kicker;
  private final String url;
  private final String filename;
  private String category;

  public GeneralConferenceAddress(
      String conference,
      int ordinal,
      String session,
      String title,
      String url,
      String filename
  ) {
    this.conference = conference;
    this.ordinal = ordinal;
    this.session = session;
    this.title = title;
    this.url = url;
    this.filename = filename;
  }

  private GeneralConferenceAddress(ResultSet result) throws SQLException {
    this(
        result.getString("conference"),
        result.getInt("ordinal"),
        result.getString("session"),
        result.getString("title"),
        result.getString("url"),
        result.getString("filename")
    );
    setSpeaker(result.getString("speaker"));
    setDescription(result.getString("description"));
    setRole(result.getString("role"));
    setKicker(result.getString("kicker"));
    setCategory(result.getString("category"));
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

  public String getSpeaker() {
    return speaker;
  }

  public void setSpeaker(String speaker) {
    // Video: https://www.churchofjesuschrist.org/study/general-conference/2020/10?lang=eng
    if (speaker == null || speaker.trim().length() > 0) {
      this.speaker = speaker;
    }
  }

  public String getTitle() {
    return title;
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
    // https://www.churchofjesuschrist.org/study/general-conference/2018/04/ministering?lang=eng
    if (role == null || role.trim().length() > 0) {
      this.role = role;
    }
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

  public String getFilename() {
    return filename;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Override
  public String getPrimaryKey() {
    return getConference() + ":" + getOrdinal();
  }

  @Override
  public int insertOrReplace(Connection connection) throws SQLException {
    final String sql =
        "INSERT OR REPLACE INTO " + Namespaces.TABLE_GENERAL_CONFERENCE_ADDRESS + " (" +
            "conference" +
            ",ordinal" +
            ",session" +
            ",speaker" +
            ",title" +
            ",description" +
            ",role" +
            ",kicker" +
            ",url" +
            ",filename" +
            ",category" +
            ") VALUES(?,?,?,?,?,?,?,?,?,?,?);";
    try (final PreparedStatement insert = connection.prepareStatement(sql)) {
      insert.setString(1, getConference());
      insert.setInt(2, getOrdinal());
      insert.setString(3, getSession());
      DatabaseUtils.setStringOrNull(insert, 4, getSpeaker());
      insert.setString(5, getTitle());
      DatabaseUtils.setStringOrNull(insert, 6, getDescription());
      DatabaseUtils.setStringOrNull(insert, 7, getRole());
      DatabaseUtils.setStringOrNull(insert, 8, getKicker());
      insert.setString(9, getUrl());
      DatabaseUtils.setStringOrNull(insert, 10, getFilename());
      DatabaseUtils.setStringOrNull(insert, 11, getCategory());
      return insert.executeUpdate();
    }
  }

  public static List<GeneralConferenceAddress> selectAll(Connection connection) throws SQLException {
    final String sql = "SELECT * FROM " + Namespaces.TABLE_GENERAL_CONFERENCE_ADDRESS + ";";
    try (final PreparedStatement select = connection.prepareStatement(sql)) {
      final ResultSet result = select.executeQuery();
      final List<GeneralConferenceAddress> addresses = new ArrayList<>();
      while (result.next()) {
        addresses.add(new GeneralConferenceAddress(result));
      }
      return addresses;
    }
  }

  public static GeneralConferenceAddress selectWithUrl(Connection connection, String url) throws SQLException {
    final String sql = "SELECT * FROM " + Namespaces.TABLE_GENERAL_CONFERENCE_ADDRESS + " WHERE url=?;";
    try (final PreparedStatement select = connection.prepareStatement(sql)) {
      select.setString(1, url);
      final ResultSet result = select.executeQuery();
      if (!result.next()) {
        return null;
      }
      return new GeneralConferenceAddress(result);
    }
  }
}
