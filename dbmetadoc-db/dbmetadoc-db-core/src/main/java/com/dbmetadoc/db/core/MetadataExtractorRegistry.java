package com.dbmetadoc.db.core;

import cn.hutool.core.collection.CollUtil;
import com.dbmetadoc.common.enums.ResultCode;
import com.dbmetadoc.common.exception.BusinessException;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据提取器注册表。
 *
 * @author mumu
 * @date 2026-03-30
 */
public class MetadataExtractorRegistry {

    private final Map<DatabaseType, MetadataExtractor> extractorMap = new EnumMap<>(DatabaseType.class);

    public MetadataExtractorRegistry(Collection<MetadataExtractor> extractors) {
        if (CollUtil.isEmpty(extractors)) {
            throw new IllegalArgumentException("Metadata extractors must not be empty");
        }
        for (MetadataExtractor extractor : extractors) {
            extractorMap.put(extractor.getDatabaseType(), extractor);
        }
    }

    public MetadataExtractor getExtractor(DatabaseType databaseType) {
        MetadataExtractor extractor = extractorMap.get(databaseType);
        if (extractor == null) {
            throw new BusinessException(ResultCode.UNSUPPORTED_DATABASE, "未找到数据库驱动实现: " + databaseType.name());
        }
        return extractor;
    }

    /**
     * 获取单个数据库类型对应的驱动描述。
     */
    public DriverDescriptor getDriverDescriptor(DatabaseType databaseType) {
        return getExtractor(databaseType).getDriverDescriptor();
    }

    /**
     * 按枚举顺序返回全部驱动描述。
     */
    public List<DriverDescriptor> listDriverDescriptors() {
        return extractorMap.keySet().stream()
                .sorted()
                .map(this::getDriverDescriptor)
                .collect(Collectors.toList());
    }
}


