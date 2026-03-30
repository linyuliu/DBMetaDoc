package com.dbmetadoc.app.service.document;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 导出字段组定义。
 *
 * @author mumu
 * @date 2026-03-30
 */
public enum ExportSection {

    DATABASE_OVERVIEW("DATABASE_OVERVIEW", "库概览", "数据库名称、类型、版本、字符集与 Schema 等摘要信息"),
    TABLE_OVERVIEW("TABLE_OVERVIEW", "表概览", "表名称、注释、引擎、字符集、排序规则等概览信息"),
    COLUMN_BASIC("COLUMN_BASIC", "列基础", "核心字段表，默认只展示字段名、类型、主键、可空、默认值、注释"),
    COLUMN_EXTENDED("COLUMN_EXTENDED", "列扩展", "在主字段表下方增加扩展补充区，不再扩宽主字段表"),
    INDEXES("INDEXES", "索引", "普通索引与唯一索引信息"),
    FOREIGN_KEYS("FOREIGN_KEYS", "外键", "外键约束与引用关系");

    private final String code;
    private final String label;
    private final String description;

    ExportSection(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static EnumSet<ExportSection> fromCodes(List<String> codes) {
        if (CollUtil.isEmpty(codes)) {
            return defaults();
        }
        EnumSet<ExportSection> sections = EnumSet.noneOf(ExportSection.class);
        for (String code : codes) {
            if (StrUtil.isBlank(code)) {
                continue;
            }
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            for (ExportSection section : values()) {
                if (section.code.equals(normalized)) {
                    sections.add(section);
                    break;
                }
            }
        }
        return sections.isEmpty() ? defaults() : sections;
    }

    public static EnumSet<ExportSection> defaults() {
        return EnumSet.of(
                DATABASE_OVERVIEW,
                TABLE_OVERVIEW,
                COLUMN_BASIC,
                INDEXES,
                FOREIGN_KEYS
        );
    }

    public static List<String> defaultCodes() {
        return defaults().stream().map(ExportSection::getCode).toList();
    }

    public static Set<String> codesOf(EnumSet<ExportSection> sections) {
        return sections.stream().map(ExportSection::getCode).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}


