package com.ericrobertbrewer.lectern.app.model;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseTable {

  int insertOrReplace(Connection connection) throws SQLException;
}
