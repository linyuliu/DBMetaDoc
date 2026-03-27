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
public class DatabaseInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    private String version;
    private String driverName;
    private String charset;
    private String collation;
    private List<TableInfo> tables;
}
