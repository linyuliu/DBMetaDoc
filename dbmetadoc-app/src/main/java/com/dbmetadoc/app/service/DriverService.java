package com.dbmetadoc.app.service;

import com.dbmetadoc.common.vo.DriverInfoResponse;
import com.dbmetadoc.db.core.MetadataExtractorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 驱动信息查询服务。
 */
@Service
@RequiredArgsConstructor
public class DriverService {

    private final MetadataExtractorRegistry metadataExtractorRegistry;

    public List<DriverInfoResponse> list() {
        return metadataExtractorRegistry.listDriverDescriptors().stream()
                .map(descriptor -> DriverInfoResponse.builder()
                        .type(descriptor.getType().name())
                        .label(descriptor.getLabel())
                        .defaultPort(descriptor.getDefaultPort())
                        .driverClass(descriptor.getDriverClass())
                        .testSql(descriptor.getTestSql())
                        .domestic(descriptor.isDomestic())
                        .mysqlLike(descriptor.isMysqlLike())
                        .pgLike(descriptor.isPgLike())
                        .oracleLike(descriptor.isOracleLike())
                        .supportsDatabase(descriptor.isSupportsDatabase())
                        .supportsSchema(descriptor.isSupportsSchema())
                        .supportsJdbcUrl(descriptor.isSupportsJdbcUrl())
                        .metadataStrategy(descriptor.getMetadataStrategy())
                        .build())
                .toList();
    }
}
