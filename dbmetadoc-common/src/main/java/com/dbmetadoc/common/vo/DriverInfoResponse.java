package com.dbmetadoc.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverInfoResponse {

    private String type;
    private String label;
    private Integer defaultPort;
    private String driverClass;
    private String testSql;
    private Boolean domestic;
    private Boolean mysqlLike;
    private Boolean pgLike;
    private Boolean oracleLike;
    private Boolean supportsDatabase;
    private Boolean supportsSchema;
    private Boolean supportsJdbcUrl;
    private String metadataStrategy;
}
