CREATE TABLE IF NOT EXISTS md_datasource_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(128) NOT NULL COMMENT '数据源名称',
    db_type VARCHAR(32) NOT NULL COMMENT '数据库类型',
    host VARCHAR(255) NOT NULL COMMENT '数据库主机地址',
    port INT NOT NULL COMMENT '数据库端口',
    database_name VARCHAR(128) NOT NULL COMMENT '数据库名或服务名',
    schema_name VARCHAR(128) DEFAULT NULL COMMENT 'Schema名称',
    username VARCHAR(128) NOT NULL COMMENT '连接用户名',
    driver_class VARCHAR(255) DEFAULT NULL COMMENT 'JDBC驱动类名',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_md_datasource_profile_name (name),
    KEY idx_md_datasource_profile_deleted (deleted)
) COMMENT='数据源模板配置表';

CREATE TABLE IF NOT EXISTS md_metadata_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cache_key VARCHAR(64) NOT NULL COMMENT '缓存指纹键',
    cache_hash VARCHAR(64) NOT NULL COMMENT '元数据内容哈希',
    db_type VARCHAR(32) NOT NULL COMMENT '数据库类型',
    database_name VARCHAR(128) NOT NULL COMMENT '数据库名或服务名',
    schema_name VARCHAR(128) DEFAULT NULL COMMENT 'Schema名称',
    metadata_json LONGTEXT NOT NULL COMMENT '元数据JSON快照',
    synced_at DATETIME NOT NULL COMMENT '最近同步时间',
    expires_at DATETIME NOT NULL COMMENT '缓存过期时间',
    UNIQUE KEY uk_md_metadata_cache_key (cache_key),
    KEY idx_md_metadata_cache_expire (expires_at)
) COMMENT='数据库元数据缓存表';
