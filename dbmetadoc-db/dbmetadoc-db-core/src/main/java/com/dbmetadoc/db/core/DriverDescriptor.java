package com.dbmetadoc.db.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 驱动展示描述。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDescriptor {

    private DatabaseType type;

    private String label;

    private Integer defaultPort;

    private String driverClass;

    private String testSql;

    private boolean domestic;

    private boolean mysqlLike;

    private boolean pgLike;

    private boolean oracleLike;

    private boolean supportsDatabase;

    private boolean supportsSchema;

    private boolean supportsJdbcUrl;

    private String metadataStrategy;
}


