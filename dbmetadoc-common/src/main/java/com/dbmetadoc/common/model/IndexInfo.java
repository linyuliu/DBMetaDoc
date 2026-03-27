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
public class IndexInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private String tableName;
    private Boolean unique;
    private List<String> columnNames;
    private String type;
}
