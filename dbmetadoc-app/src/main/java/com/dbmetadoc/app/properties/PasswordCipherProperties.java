package com.dbmetadoc.app.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模板密码加密配置。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@ConfigurationProperties(prefix = "dbmetadoc.security")
public class PasswordCipherProperties {

    private String passwordSecret = "dbmetadoc-dev-secret-20260328";
}


