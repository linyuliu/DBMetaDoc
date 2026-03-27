package com.dbmetadoc.common.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceDetailResponse {

    private Long id;
    private String name;
    private String dbType;
    private String jdbcUrl;
    private String host;
    private Integer port;
    private String database;
    private String schema;
    private String username;
    private String driverClass;
    private String testSql;
    private String remark;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
