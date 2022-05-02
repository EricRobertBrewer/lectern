package com.ericrobertbrewer.lectern.app.model;

import com.ericrobertbrewer.lectern.Namespaces;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GeneralConference implements DatabaseTable {

  public static final String CREATE_STMT =
    "CREATE TABLE IF NOT EXISTS " + Namespaces.TABLE_GENERAL_CONFERENCE + " (" +
      "conference TEXT PRIMARY KEY" + // 2022-04
      ", count INTEGER NOT NULL" + // 38
      ", url TEXT NOT NULL" + // https://www.churchofjesuschrist.org/study/general-conference/2022/04?lang=eng
      ");";

  private final String conference;
  private final int count;
  private final String url;

  public GeneralConference(String conference, int count, String url) {
    this.conference = conference;
    this.count = count;
    this.url = url;
  }

  private GeneralConference(ResultSet result) throws SQLException {
    this(
      result.getString("conference"),
      result.getInt("count"),
      result.getString("url")
    );
  }

  public String getConference() {
    return conference;
  }

  public int getCount() {
    return count;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public String getPrimaryKey() {
    return getConference();
  }

  @Override
  public int insertOrReplace(Connection connection) throws SQLException {
    final String sql =
      "INSERT INTO " + Namespaces.TABLE_GENERAL_CONFERENCE + " (" +
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

  public static GeneralConference select(Connection connection, String conference) throws SQLException {
    final String sql = "SELECT * FROM " + Namespaces.TABLE_GENERAL_CONFERENCE + " WHERE conference=?;";
    try (final PreparedStatement select = connection.prepareStatement(sql)) {
      select.setString(1, conference);
      final ResultSet result = select.executeQuery();
      if (!result.next()) {
        return null;
      }
      return new GeneralConference(result);
    }
  }
}
