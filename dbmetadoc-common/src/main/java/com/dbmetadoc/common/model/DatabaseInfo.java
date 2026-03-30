package com.dbmetadoc.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 数据库信息对象。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private String version;
    private String driverName;
    private String databaseName;
    private String schemaName;
    private String catalogName;
    private String charset;
    private String collation;
    private List<TableInfo> tables;
}
