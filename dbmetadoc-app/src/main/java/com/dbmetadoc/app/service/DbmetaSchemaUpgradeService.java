package com.dbmetadoc.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * dbmeta 管理库轻量升级器。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DbmetaSchemaUpgradeService implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        upgradeDatasourcePasswordColumn();
    }

    private void upgradeDatasourcePasswordColumn() {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'md_datasource_profile'
                  AND COLUMN_NAME = 'password_cipher'
                """, Integer.class);
        if (count != null && count > 0) {
            return;
        }
        log.info("检测到 md_datasource_profile 缺少 password_cipher 字段，开始执行轻量升级");
        jdbcTemplate.execute("""
                ALTER TABLE md_datasource_profile
                ADD COLUMN password_cipher VARCHAR(1024) DEFAULT NULL COMMENT '加密后的连接密码'
                AFTER username
                """);
        log.info("md_datasource_profile.password_cipher 字段升级完成");
    }
}


