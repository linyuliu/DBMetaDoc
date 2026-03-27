package com.dbmetadoc.app.service;

import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.db.core.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectionInfoResolverTest {

    private final ConnectionInfoResolver resolver = new ConnectionInfoResolver();

    @Test
    void shouldResolveMysqlFieldsFromJdbcUrlWhenStructuredFieldsMissing() {
        ConnectionRequest request = new ConnectionRequest();
        request.setDbType("MYSQL");
        request.setJdbcUrl("jdbc:mysql://127.0.0.1:3307/demo_db?useSSL=false");
        request.setUsername("root");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals(DatabaseType.MYSQL, resolved.getType());
        assertEquals("127.0.0.1", resolved.getHost());
        assertEquals(3307, resolved.getPort());
        assertEquals("demo_db", resolved.getDatabase());
    }

    @Test
    void shouldPreferStructuredFieldsOverJdbcUrl() {
        ConnectionRequest request = new ConnectionRequest();
        request.setDbType("POSTGRESQL");
        request.setJdbcUrl("jdbc:postgresql://10.0.0.1:5432/raw_db?currentSchema=raw_schema");
        request.setHost("192.168.1.11");
        request.setPort(15432);
        request.setDatabase("app_db");
        request.setSchema("biz");
        request.setUsername("postgres");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals("192.168.1.11", resolved.getHost());
        assertEquals(15432, resolved.getPort());
        assertEquals("app_db", resolved.getDatabase());
        assertEquals("biz", resolved.getSchema());
    }

    @Test
    void shouldDefaultOracleLikeSchemaToUppercaseUsername() {
        ConnectionRequest request = new ConnectionRequest();
        request.setDbType("ORACLE");
        request.setHost("127.0.0.1");
        request.setPort(1521);
        request.setDatabase("ORCLPDB1");
        request.setUsername("system");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals("SYSTEM", resolved.getSchema());
    }

    @Test
    void shouldKeepKingbaseSchemaEmptyBeforeCompatibilityModeDetected() {
        ConnectionRequest request = new ConnectionRequest();
        request.setDbType("KINGBASE");
        request.setJdbcUrl("jdbc:kingbase8://127.0.0.1:54321/demo_db");
        request.setUsername("system");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertNull(resolved.getSchema());
    }

    @Test
    void shouldRejectMissingHostAndDatabaseWhenJdbcUrlCannotSupplyThem() {
        ConnectionRequest request = new ConnectionRequest();
        request.setDbType("MYSQL");
        request.setUsername("root");
        request.setPassword("123456");

        assertThrows(BusinessException.class, () -> resolver.resolve(request));
    }
}
