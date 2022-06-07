package com.ericrobertbrewer.lectern.scrape.db;

import com.ericrobertbrewer.lectern.scrape.Namespaces;

import java.sql.*;

public final class DatabaseUtils {

  public static void main(String[] args) throws SQLException {
    try (Connection connection = connect(Namespaces.DATABASE_PATH_APP_DEFAULT)) {
      // Put database operations here.
    }
  }

  static {
    // Ensure that a Java database connection class exists.
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "Unable to find suitable SQLite driver (JDBC)." +
              " Perhaps try adding as a dependency `org.xerial:sqlite-jdbc:3.x.x`.",
          e);
    }
  }

  private DatabaseUtils() {
  }

  public static Connection connect(String fileName) throws SQLException {
    return DriverManager.getConnection("jdbc:sqlite:" + fileName);
  }

  public static void executeSql(Connection connection, String sql) throws SQLException {
    try (Statement create = connection.createStatement()) {
      create.execute(sql);
    }
  }

  public static void setStringOrNull(PreparedStatement s, int parameterIndex, String value) throws SQLException {
    if (value != null) {
      s.setString(parameterIndex, value);
    } else {
      s.setNull(parameterIndex, Types.VARCHAR);
    }
  }
}