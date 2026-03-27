package com.dbmetadoc.db;

import com.dbmetadoc.common.model.DatabaseInfo;

import java.sql.Connection;
import java.sql.SQLException;

public interface MetadataExtractor {
    DatabaseInfo extract(Connection connection, String database) throws SQLException;
}
