package com.dbmetadoc.generator;

import com.dbmetadoc.common.model.DatabaseInfo;

public interface DocumentGenerator {
    byte[] generate(DatabaseInfo databaseInfo, String title) throws Exception;
    String getFormat();
}
