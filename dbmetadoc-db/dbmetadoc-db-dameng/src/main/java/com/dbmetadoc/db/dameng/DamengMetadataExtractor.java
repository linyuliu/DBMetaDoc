package com.dbmetadoc.db.dameng;

import com.dbmetadoc.db.core.AbstractOracleLikeTableExtractor;
import com.dbmetadoc.db.core.DatabaseConnectionInfo;
import com.dbmetadoc.db.core.DatabaseType;

/**
 * 达梦元数据提取实现。
 */
public class DamengMetadataExtractor extends AbstractOracleLikeTableExtractor {

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.DAMENG;
    }

    @Override
    protected String resolveOwner(DatabaseConnectionInfo connectionInfo) {
        return super.resolveOwner(connectionInfo);
    }

    @Override
    protected String columnSql(boolean useAllViews) {
        if (useAllViews) {
            return """
                    SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE,
                           c.NULLABLE, c.DATA_DEFAULT, c.COLUMN_ID, NULL AS IDENTITY_COLUMN, NULL AS VIRTUAL_COLUMN, cm.COMMENTS
                    FROM ALL_TAB_COLUMNS c
                    LEFT JOIN ALL_COL_COMMENTS cm
                      ON cm.OWNER = c.OWNER
                     AND cm.TABLE_NAME = c.TABLE_NAME
                     AND cm.COLUMN_NAME = c.COLUMN_NAME
                    WHERE c.OWNER = ?
                    ORDER BY c.TABLE_NAME, c.COLUMN_ID
                    """;
        }
        return """
                SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.DATA_PRECISION, c.DATA_SCALE,
                       c.NULLABLE, c.DATA_DEFAULT, c.COLUMN_ID, NULL AS IDENTITY_COLUMN, NULL AS VIRTUAL_COLUMN, cm.COMMENTS
                FROM USER_TAB_COLUMNS c
                LEFT JOIN USER_COL_COMMENTS cm
                  ON cm.TABLE_NAME = c.TABLE_NAME
                 AND cm.COLUMN_NAME = c.COLUMN_NAME
                ORDER BY c.TABLE_NAME, c.COLUMN_ID
                """;
    }
}
