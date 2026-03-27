package com.dbmetadoc.app.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "dbmetadoc.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173"));

    private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST"));

    private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

    private Boolean allowCredentials = true;

    private Long maxAge = 3600L;
}
