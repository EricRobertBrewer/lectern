package com.ericrobertbrewer.lectern.app.model;

import com.ericrobertbrewer.lectern.Namespaces;
import com.ericrobertbrewer.lectern.db.DatabaseUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GeneralConferenceAddressRef implements DatabaseTable {

  /**
   * Ultra-rarely when a note refers to the title.
   *
   * @see <a href="https://www.churchofjesuschrist.org/study/general-conference/1975/10/the-welfare-production-distribution-department?lang=eng">www.churchofjesuschrist.org/study/general-conference/1975/10/the-welfare-production-distribution-department?lang=eng</a>
   */
  public static final int LINE_TITLE = -2;
  /**
   * Used when a note refers to the kicker.
   *
   * @see <a href="https://www.churchofjesuschrist.org/study/general-conference/2007/10/o-remember-remember?lang=eng">O Remember, Remember</a>
   */
  public static final int LINE_KICKER = -1;

  public static final String CREATE_STMT =
    "CREATE TABLE IF NOT EXISTS " + Namespaces.TABLE_GENERAL_CONFERENCE_ADDRESS_REF + " (" +
      "conference TEXT NOT NULL" + // 2018-04
      ", ordinal INTEGER NOT NULL" + // 27
      ", url TEXT NOT NULL" + // http://devotional.byuh.edu/node/158
      ", lines TEXT NOT NULL" + // 16,26
      ", notes TEXT DEFAULT NULL" + // `Steven C. Wheelwright, “The Power of Small and Simple Things”...|Steven...`
      ", PRIMARY KEY (conference, ordinal, url)" +
      ");";

  private final String conference;
  private final int ordinal;
  private final String url;
  private final int[] lines;
  private final String[] notes;

  public GeneralConferenceAddressRef(String conference, int ordinal, String url, int[] lines, String[] notes) {
    this.conference = conference;
    this.ordinal = ordinal;
    this.url = url;
    this.lines = lines;
    this.notes = notes;
  }

  private GeneralConferenceAddressRef(ResultSet result) throws SQLException {
    this(
      result.getString("conference"),
      result.getInt("ordinal"),
      result.getString("url"),
      Arrays.stream(result.getString("lines").split(",")).mapToInt(Integer::parseInt).toArray(),
      result.getString("notes") != null ? result.getString("notes").split("\\|") : null
    );
  }

  public String getConference() {
    return conference;
  }

  public int getOrdinal() {
    return ordinal;
  }

  public String getUrl() {
    return url;
  }

  public int[] getLines() {
    return lines;
  }

  public String[] getNotes() {
    return notes;
  }

  @Override
  public String getPrimaryKey() {
    return getConference() + ":" + getOrdinal() + ":" + getUrl();
  }

  @Override
  public int insertOrReplace(Connection connection) throws SQLException {
    final String sql =
      "INSERT OR REPLACE INTO " + Namespaces.TABLE_GENERAL_CONFERENCE_ADDRESS_REF +
        " (conference,ordinal,url,lines,notes)" +
        " VALUES(?,?,?,?,?);";
    try (final PreparedStatement insert = connection.prepareStatement(sql)) {
      insert.setString(1, getConference());
      insert.setInt(2, getOrdinal());
      insert.setString(3, getUrl());
      insert.setString(4,
        Arrays.stream(getLines()).mapToObj(String::valueOf).collect(Collectors.joining(",")));
      DatabaseUtils.setStringOrNull(insert, 5,
        getNotes() != null ? String.join("|", getNotes()) : null);
      return insert.executeUpdate();
    }
  }

  public static List<GeneralConferenceAddressRef> selectWithUrl(Connection connection, String url) throws SQLException {
    final String sql = "SELECT * FROM " + Namespaces.TABLE_GENERAL_CONFERENCE_ADDRESS_REF + " WHERE url=?;";
    try (final PreparedStatement select = connection.prepareStatement(sql)) {
      select.setString(1, url);
      final ResultSet result = select.executeQuery();
      final List<GeneralConferenceAddressRef> refs = new ArrayList<>();
      while (result.next()) {
        refs.add(new GeneralConferenceAddressRef(result));
      }
      return refs;
    }
  }
}
