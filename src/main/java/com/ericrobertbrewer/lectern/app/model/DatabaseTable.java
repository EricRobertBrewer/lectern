package com.ericrobertbrewer.lectern.app.model;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseTable {

  String getPrimaryKey();
  int insertOrReplace(Connection connection) throws SQLException;
}
