package com.dbmetadoc.common.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 数据源模板实体。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String dbType;
    private String host;
    private Integer port;
    private String databaseName;
    private String schemaName;
    private String username;
    private String passwordCipher;
    private String driverClass;
    private String remark;
    private Boolean enabled;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
