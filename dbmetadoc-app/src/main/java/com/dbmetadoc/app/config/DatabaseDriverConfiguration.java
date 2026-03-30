package com.dbmetadoc.app.config;

import com.dbmetadoc.db.MetadataModules;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 数据库驱动装配配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Configuration
public class DatabaseDriverConfiguration {

    @Bean
    public MetadataExtractorRegistry metadataExtractorRegistry() {
        return MetadataModules.createRegistry();
    }
}
