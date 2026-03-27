package com.dbmetadoc.common.model;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String tableName;
    private String columnName;
    private String referencedTable;
    private String referencedColumn;
}
