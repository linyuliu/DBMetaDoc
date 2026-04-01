package com.dbmetadoc.app.service;

import com.dbmetadoc.common.dto.ConnectionRequest;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;
import com.dbmetadoc.db.core.DatabaseType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        assertEquals("true", resolved.getJdbcParameters().get("useInformationSchema"));
        assertEquals("false", resolved.getJdbcUrlParts().getParameters().get("useSSL"));
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
    void shouldResolveMysqlTransportVariantAndPickFirstHost() {
        ConnectionRequest request = new ConnectionRequest();
        request.setJdbcUrl("jdbc:mysql:loadbalance://primary-db:3306,replica-db:3307/demo_db?useSSL=false");
        request.setUsername("root");
        request.setPassword("123456");
        ConnectionRequest srvRequest = new ConnectionRequest();
        srvRequest.setJdbcUrl("jdbc:mysql+srv://cluster.example.com/demo_srv");
        srvRequest.setUsername("root");
        srvRequest.setPassword("123456");

        var resolved = resolver.resolve(request);
        var srvResolved = resolver.resolve(srvRequest);

        assertEquals(DatabaseType.MYSQL, resolved.getType());
        assertEquals("primary-db", resolved.getHost());
        assertEquals(3306, resolved.getPort());
        assertEquals("demo_db", resolved.getDatabase());
        assertEquals("false", resolved.getJdbcParameters().get("useSSL"));
        assertEquals("cluster.example.com", srvResolved.getHost());
        assertEquals("demo_srv", srvResolved.getDatabase());
    }

    @Test
    void shouldMapMariadbAndTidbToMysql() {
        ConnectionRequest mariadbRequest = new ConnectionRequest();
        mariadbRequest.setJdbcUrl("jdbc:mariadb://127.0.0.1:3306/maria_db");
        mariadbRequest.setUsername("root");
        mariadbRequest.setPassword("123456");

        ConnectionRequest tidbRequest = new ConnectionRequest();
        tidbRequest.setJdbcUrl("jdbc:tidb://127.0.0.1:4000/tidb_db");
        tidbRequest.setUsername("root");
        tidbRequest.setPassword("123456");

        var mariadbResolved = resolver.resolve(mariadbRequest);
        var tidbResolved = resolver.resolve(tidbRequest);

        assertEquals(DatabaseType.MYSQL, mariadbResolved.getType());
        assertEquals("maria_db", mariadbResolved.getDatabase());
        assertEquals(DatabaseType.MYSQL, tidbResolved.getType());
        assertEquals(4000, tidbResolved.getPort());
    }

    @Test
    void shouldResolvePostgresqlCompatibleVendorsAndSchemaKeysCaseInsensitively() {
        ConnectionRequest request = new ConnectionRequest();
        request.setJdbcUrl("jdbc:log4jdbc:edb://primary-pg:5444,standby-pg:5445/app_db;CurrentSchema=reporting");
        request.setUsername("postgres");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals(DatabaseType.POSTGRESQL, resolved.getType());
        assertEquals("primary-pg", resolved.getHost());
        assertEquals(5444, resolved.getPort());
        assertEquals("app_db", resolved.getDatabase());
        assertEquals("reporting", resolved.getSchema());
    }

    @Test
    void shouldDefaultPostgresqlSchemaToPublicWhenMissing() {
        ConnectionRequest request = new ConnectionRequest();
        request.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/demo_db");
        request.setUsername("postgres");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals("public", resolved.getSchema());
    }

    @Test
    void shouldResolveOracleServiceNameUrl() {
        ConnectionRequest request = new ConnectionRequest();
        request.setJdbcUrl("jdbc:oracle:thin:@//10.10.10.8:1522/ORCLPDB1");
        request.setUsername("system");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals(DatabaseType.ORACLE, resolved.getType());
        assertEquals("10.10.10.8", resolved.getHost());
        assertEquals(1522, resolved.getPort());
        assertEquals("ORCLPDB1", resolved.getDatabase());
        assertEquals("SYSTEM", resolved.getSchema());
        assertEquals("true", resolved.getJdbcParameters().get("remarksReporting"));
        assertEquals("jdbc:oracle:thin:@//10.10.10.8:1522/ORCLPDB1?remarksReporting=true", resolved.getResolvedJdbcUrl());
    }

    @Test
    void shouldResolveOracleSidUrl() {
        ConnectionRequest request = new ConnectionRequest();
        request.setJdbcUrl("jdbc:oracle:thin:@10.10.10.9:1521:ORCL");
        request.setUsername("system");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals(DatabaseType.ORACLE, resolved.getType());
        assertEquals("10.10.10.9", resolved.getHost());
        assertEquals(1521, resolved.getPort());
        assertEquals("ORCL", resolved.getDatabase());
        assertTrue(resolved.isSidMode());
        assertEquals("jdbc:oracle:thin:@10.10.10.9:1521:ORCL?remarksReporting=true", resolved.getResolvedJdbcUrl());
    }

    @Test
    void shouldResolveOracleDescriptionUrl() {
        ConnectionRequest request = new ConnectionRequest();
        request.setJdbcUrl("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=192.168.10.15)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=orclpdb1)));oracle.net.CONNECT_TIMEOUT=3000");
        request.setUsername("system");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals(DatabaseType.ORACLE, resolved.getType());
        assertEquals("192.168.10.15", resolved.getHost());
        assertEquals(1521, resolved.getPort());
        assertEquals("orclpdb1", resolved.getDatabase());
        assertEquals("3000", resolved.getJdbcParameters().get("oracle.net.CONNECT_TIMEOUT"));
        assertEquals("true", resolved.getJdbcParameters().get("remarksReporting"));
    }

    @Test
    void shouldSupplementMetadataParametersWithoutOverwritingExplicitValues() {
        ConnectionRequest mysqlRequest = new ConnectionRequest();
        mysqlRequest.setDbType("MYSQL");
        mysqlRequest.setHost("127.0.0.1");
        mysqlRequest.setPort(3306);
        mysqlRequest.setDatabase("dbmeta");
        mysqlRequest.setUsername("root");
        mysqlRequest.setPassword("123456");

        ConnectionRequest mysqlCustomRequest = new ConnectionRequest();
        mysqlCustomRequest.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/dbmeta?useInformationSchema=false");
        mysqlCustomRequest.setUsername("root");
        mysqlCustomRequest.setPassword("123456");

        var supplemented = resolver.resolve(mysqlRequest);
        var preserved = resolver.resolve(mysqlCustomRequest);

        assertEquals("true", supplemented.getJdbcParameters().get("useInformationSchema"));
        assertTrue(supplemented.getResolvedJdbcUrl().contains("useInformationSchema=true"));
        assertEquals("false", preserved.getJdbcParameters().get("useInformationSchema"));
        assertEquals("false", preserved.getJdbcUrlParts().getParameters().get("useInformationSchema"));
    }

    @Test
    void shouldRejectConflictingExplicitDbTypeAndJdbcUrl() {
        ConnectionRequest request = new ConnectionRequest();
        request.setDbType("MYSQL");
        request.setJdbcUrl("jdbc:postgresql://127.0.0.1:5432/demo_db");
        request.setUsername("postgres");
        request.setPassword("123456");

        BusinessException exception = assertThrows(BusinessException.class, () -> resolver.resolve(request));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("PostgreSQL"));
        assertTrue(exception.getMessage().contains("MySQL"));
    }

    @Test
    void shouldRejectKnownUnsupportedVendor() {
        ConnectionRequest request = new ConnectionRequest();
        request.setJdbcUrl("jdbc:sqlserver://sql-host:1433;databaseName=erp");
        request.setUsername("sa");
        request.setPassword("123456");

        BusinessException exception = assertThrows(BusinessException.class, () -> resolver.resolve(request));

        assertEquals(ResultCode.UNSUPPORTED_DATABASE.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("SQL Server"));
    }

    @Test
    void shouldFallbackToStructuredFieldsWhenJdbcUrlUnknown() {
        ConnectionRequest request = new ConnectionRequest();
        request.setDbType("MYSQL");
        request.setJdbcUrl("jdbc:customdb://opaque-host/ignored");
        request.setHost("manual-host");
        request.setPort(3308);
        request.setDatabase("manual_db");
        request.setUsername("root");
        request.setPassword("123456");

        var resolved = resolver.resolve(request);

        assertEquals(DatabaseType.MYSQL, resolved.getType());
        assertEquals("manual-host", resolved.getHost());
        assertEquals(3308, resolved.getPort());
        assertEquals("manual_db", resolved.getDatabase());
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
