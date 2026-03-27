package com.dbmetadoc.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String comment;
    private String schema;
    private String primaryKey;
    private String tableType;
    private List<ColumnInfo> columns;
    private List<IndexInfo> indexes;
    private List<ForeignKeyInfo> foreignKeys;
}
