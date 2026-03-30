package com.dbmetadoc.app.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 字体探测配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@ConfigurationProperties(prefix = "dbmetadoc.font")
public class FontProfileProperties {

    private List<String> additionalDirectories = new ArrayList<>();
}


