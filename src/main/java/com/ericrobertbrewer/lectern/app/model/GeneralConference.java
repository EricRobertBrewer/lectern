package com.ericrobertbrewer.lectern.app.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GeneralConference implements DatabaseTable {

  public static final String TABLE_GENERAL_CONFERENCE = "GeneralConference";
  public static final String CREATE_STMT = "CREATE TABLE IF NOT EXISTS " + TABLE_GENERAL_CONFERENCE + " (" +
    "conference TEXT PRIMARY KEY" + // 2022-04
    ", count INTEGER NOT NULL" + // 38
    ", url TEXT NOT NULL" + // https://www.churchofjesuschrist.org/study/general-conference/2022/04?lang=eng
    ");";

  public static GeneralConference select(Connection connection, String conference) throws SQLException {
    final String sql = "SELECT *" +
      " FROM " + TABLE_GENERAL_CONFERENCE +
      " WHERE conference=?;";
    try (final PreparedStatement select = connection.prepareStatement(sql)) {
      select.setString(1, conference);
      final ResultSet result = select.executeQuery();
      if (!result.next()) {
        return null;
      }
      final int count = result.getInt("count");
      final String url = result.getString("url");
      return new GeneralConference(conference, count, url);
    }
  }

  private final String conference;
  private int count;
  private String url;

  public GeneralConference(String conference, int count, String url) {
    this.conference = conference;
    this.count = count;
    this.url = url;
  }

  public String getConference() {
    return conference;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public int insertOrReplace(Connection connection) throws SQLException {
    final String sql = "INSERT" +
      " INTO " + TABLE_GENERAL_CONFERENCE + " (" +
      "conference" +
      ",count" +
      ",url" +
      ") VALUES(?,?,?);";
    try (final PreparedStatement insert = connection.prepareStatement(sql)) {
      insert.setString(1, getConference());
      insert.setInt(2, getCount());
      insert.setString(3, getUrl());
      return insert.executeUpdate();
    }
  }
}
