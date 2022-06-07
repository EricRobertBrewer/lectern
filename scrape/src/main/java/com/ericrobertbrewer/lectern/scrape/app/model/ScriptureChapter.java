package com.ericrobertbrewer.lectern.scrape.app.model;

import com.ericrobertbrewer.lectern.scrape.Namespaces;
import com.ericrobertbrewer.lectern.scrape.db.DatabaseUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ScriptureChapter implements DatabaseTable {

  public static final String CREATE_STMT =
      "CREATE TABLE IF NOT EXISTS " + Namespaces.TABLE_SCRIPTURE_CHAPTER + " (" +
          "path TEXT PRIMARY KEY" + // ot/gen/1; bofm/introduction
          ", testament TEXT NOT NULL" + // ot; bofm
          ", book TEXT NOT NULL" + // gen; introduction
          ", chapter TEXT DEFAULT NULL" + // 1; NULL
          ");";
  public static final String PATH_DELIMITER = "/";

  final String path;
  final String testament;
  final String book;
  final String chapter;

  public ScriptureChapter(String path, String testament, String book, String chapter) {
    this.path = path;
    this.testament = testament;
    this.book = book;
    this.chapter = chapter;
  }

  private ScriptureChapter(ResultSet result) throws SQLException {
    this(
        result.getString("path"),
        result.getString("testament"),
        result.getString("book"),
        result.getString("chapter")
    );
  }

  public String getPath() {
    return path;
  }

  public String getTestament() {
    return testament;
  }

  public String getBook() {
    return book;
  }

  public String getChapter() {
    return chapter;
  }

  @Override
  public String getPrimaryKey() {
    return getPath();
  }

  public File getChapterFile(File appFolder) {
    final String[] components = getPath().split(PATH_DELIMITER);
    File parent = appFolder;
    for (int i = 0; i < components.length - 1; i++) {
      // Create folders.
      final File componentFolder = new File(parent, components[i]);
      if (!componentFolder.exists() && !componentFolder.mkdirs()) {
        throw new RuntimeException("Unable to create folder: " + componentFolder.getName());
      }
      parent = componentFolder;
    }
    return new File(parent, components[components.length - 1] + ".txt");
  }

  @Override
  public int insertOrReplace(Connection connection) throws SQLException {
    final String sql =
        "INSERT INTO " + Namespaces.TABLE_SCRIPTURE_CHAPTER +
            " (path,testament,book,chapter)" +
            " VALUES(?,?,?,?);";
    try (final PreparedStatement insert = connection.prepareStatement(sql)) {
      insert.setString(1, getPath());
      insert.setString(2, getTestament());
      insert.setString(3, getBook());
      DatabaseUtils.setStringOrNull(insert, 4, getChapter());
      return insert.executeUpdate();
    }
  }

  public static ScriptureChapter select(Connection connection, String path) throws SQLException {
    final String sql = "SELECT * FROM " + Namespaces.TABLE_SCRIPTURE_CHAPTER + " WHERE path=?;";
    try (final PreparedStatement select = connection.prepareStatement(sql)) {
      select.setString(1, path);
      final ResultSet result = select.executeQuery();
      if (!result.next()) {
        return null;
      }
      return new ScriptureChapter(result);
    }
  }
}
