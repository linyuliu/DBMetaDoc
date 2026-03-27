package com.dbmetadoc.app.service;

import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;
import com.dbmetadoc.db.kingbase.KingbaseMetadataExtractor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KingbaseMetadataExtractorTest {

    private final KingbaseMetadataExtractor extractor = new KingbaseMetadataExtractor();

    @Test
    void shouldUseOracleTestSqlWhenKingbaseRunsInOracleMode() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement("SHOW database_mode")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("oracle");

        String sql = extractor.resolveTestSql(connection, DatabaseConnectionInfo.builder()
                .type(DatabaseType.KINGBASE)
                .host("127.0.0.1")
                .port(54321)
                .database("demo")
                .username("system")
                .password("123456")
                .build());

        assertEquals("SELECT 1 FROM DUAL", sql);
    }

    @Test
    void shouldFallbackToSelectOneWhenKingbaseRunsInMysqlMode() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement("SHOW database_mode")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("mysql");

        String sql = extractor.resolveTestSql(connection, DatabaseConnectionInfo.builder()
                .type(DatabaseType.KINGBASE)
                .host("127.0.0.1")
                .port(54321)
                .database("demo")
                .username("system")
                .password("123456")
                .build());

        assertEquals("SELECT 1", sql);
    }
}
