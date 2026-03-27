package com.dbmetadoc.app.service;

import com.dbmetadoc.common.model.ColumnInfo;
import com.dbmetadoc.db.core.JdbcJavaTypeResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcJavaTypeResolverTest {

    @Test
    void shouldMapMysqlTinyintOneToBoolean() {
        ColumnInfo columnInfo = ColumnInfo.builder()
                .type("tinyint")
                .rawType("tinyint(1)")
                .length(1)
                .build();

        assertEquals("Boolean", JdbcJavaTypeResolver.resolveMysqlLike(columnInfo));
    }

    @Test
    void shouldMapOracleNumberWithoutScaleByPrecision() {
        ColumnInfo columnInfo = ColumnInfo.builder()
                .type("NUMBER")
                .rawType("NUMBER(18)")
                .precision(18)
                .scale(0)
                .build();

        assertEquals("Long", JdbcJavaTypeResolver.resolveOracleLike(columnInfo));
    }

    @Test
    void shouldFallbackUnknownDomesticTypeToString() {
        ColumnInfo columnInfo = ColumnInfo.builder()
                .type("hyperloglog")
                .rawType("hyperloglog")
                .build();

        assertEquals("String", JdbcJavaTypeResolver.resolveGeneric(columnInfo));
    }

    @Test
    void shouldMapKingbaseModesSeparately() {
        ColumnInfo mysqlColumn = ColumnInfo.builder()
                .type("tinyint")
                .rawType("tinyint(1)")
                .length(1)
                .build();
        ColumnInfo oracleColumn = ColumnInfo.builder()
                .type("NUMBER")
                .rawType("NUMBER(10)")
                .precision(10)
                .scale(0)
                .build();

        assertEquals("Boolean", JdbcJavaTypeResolver.resolveKingbase(mysqlColumn, "mysql"));
        assertEquals("Long", JdbcJavaTypeResolver.resolveKingbase(oracleColumn, "oracle"));
    }
}
