package com.dbmetadoc.app.config;

import com.dbmetadoc.db.MetadataModules;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseDriverConfiguration {

    @Bean
    public MetadataExtractorRegistry metadataExtractorRegistry() {
        return MetadataModules.createRegistry();
    }
}
