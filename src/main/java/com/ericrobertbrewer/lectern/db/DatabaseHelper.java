package com.ericrobertbrewer.lectern.db;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseHelper {

  static {
    // Ensure that a Java database connection class exists.
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to find suitable SQLite driver (JDBC). Perhaps try adding as a dependency `org.xerial:sqlite-jdbc:3.x.x`.", e);
    }
  }

  private final Logger logger = Logger.getLogger(DatabaseHelper.class.getName());
  private Connection connection = null;

  public DatabaseHelper() {
  }

  public Connection getConnection() {
    return connection;
  }

  public void connect(String fileName) {
    try {
      connection = DriverManager.getConnection("jdbc:sqlite:" + fileName);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Unable to connect to database: `" + fileName + "`.", e);
    }
  }

  public boolean isConnected() {
    if (connection == null) {
      return false;
    }
    try {
      return !connection.isClosed();
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Unable to query whether database connection is closed.", e);
    }
    return false;
  }

  public void close() {
    try {
      connection.close();
      connection = null;
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Unable to close database connection.", e);
    }
  }

  public void executeSql(String sql) {
    try (Statement create = getConnection().createStatement()) {
      create.execute(sql);
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Unable to execute SQL.", e);
    }
  }

  public boolean recordExists(String table, String key, String value) throws SQLException {
    final PreparedStatement select = getConnection().prepareStatement(
      "SELECT " + key + " FROM " + table + " WHERE " + key + "=?;");
    select.setString(1, value);
    final ResultSet result = select.executeQuery();
    final boolean exists = result.next();
    select.close();  // Also closes `result`.
    return exists;
  }

  public static int getIntOrNull(ResultSet result, String columnLabel, int nullValue) throws SQLException {
    final int value = result.getInt(columnLabel);
    if (result.wasNull()) {
      return nullValue;
    }
    return value;
  }

  public static long getLongOrNull(ResultSet result, String columnLabel, long nullValue) throws SQLException {
    final long value = result.getLong(columnLabel);
    if (result.wasNull()) {
      return nullValue;
    }
    return value;
  }

  public static void setIntOrNull(PreparedStatement s, int parameterIndex, int value, int nullValue) throws SQLException {
    if (value != nullValue) {
      s.setInt(parameterIndex, value);
    } else {
      s.setNull(parameterIndex, Types.INTEGER);
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