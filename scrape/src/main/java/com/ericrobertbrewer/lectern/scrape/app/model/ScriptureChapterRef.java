package com.ericrobertbrewer.lectern.scrape.app.model;

import com.ericrobertbrewer.lectern.scrape.Namespaces;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ScriptureChapterRef implements DatabaseTable {

  public static final String CREATE_STMT =
      "CREATE TABLE IF NOT EXISTS " + Namespaces.TABLE_SCRIPTURE_CHAPTER_REF + " (" +
          "path TEXT NOT NULL" + // ot/gen/1
          ", lines TEXT NOT NULL" + // 1,5
          ", url TEXT NOT NULL" + // https://www.churchofjesuschrist.org/study/scriptures/tg/time?lang=eng
          ", PRIMARY KEY (path, url)" +
          ");";

  private static final String LINE_DELIMITER = ",";

  final String path;
  final int[] lines;
  final String url;

  public ScriptureChapterRef(String path, int[] lines, String url) {
    this.path = path;
    this.lines = lines;
    this.url = url;
  }

  public String getPath() {
    return path;
  }

  public int[] getLines() {
    return lines;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public String getPrimaryKey() {
    return getPath() + ":" + getUrl();
  }

  @Override
  public int insertOrReplace(Connection connection) throws SQLException {
    final String sql =
        "INSERT OR REPLACE INTO " + Namespaces.TABLE_SCRIPTURE_CHAPTER_REF +
            " (path,lines,url)" +
            " VALUES(?,?,?);";
    try (final PreparedStatement insert = connection.prepareStatement(sql)) {
      insert.setString(1, getPath());
      insert.setString(2,
          Arrays.stream(getLines())
              .mapToObj(String::valueOf)
              .collect(Collectors.joining(LINE_DELIMITER)));
      insert.setString(3, getUrl());
      return insert.executeUpdate();
    }
  }
}
