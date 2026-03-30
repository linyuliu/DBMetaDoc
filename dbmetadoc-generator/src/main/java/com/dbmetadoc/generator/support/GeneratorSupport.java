package com.dbmetadoc.generator.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.common.model.ColumnInfo;

import java.util.Collection;
import java.util.List;

/**
 * 生成器共享工具。
 *
 * @author mumu
 * @date 2026-03-30
 */
public final class GeneratorSupport {

    private static final String YES = "是";
    private static final String NO = "否";
    private static final String EMPTY = "";

    private GeneratorSupport() {
    }

    public static String safeText(String value) {
        return StrUtil.blankToDefault(value, EMPTY);
    }

    public static String defaultText(String value, String defaultValue) {
        return StrUtil.blankToDefault(value, safeText(defaultValue));
    }

    public static boolean hasText(String value) {
        return StrUtil.isNotBlank(value);
    }

    public static boolean hasItems(Collection<?> values) {
        return CollUtil.isNotEmpty(values);
    }

    public static String yesNo(Boolean value) {
        if (BooleanUtil.isTrue(value)) {
            return YES;
        }
        if (BooleanUtil.isFalse(value)) {
            return NO;
        }
        return EMPTY;
    }

    public static String safeNumber(Number value) {
        if (ObjectUtil.isNull(value)) {
            return EMPTY;
        }
        return NumberUtil.toStr(value);
    }

    public static String joinChinese(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return EMPTY;
        }
        return StrUtil.join("，", values);
    }

    public static String buildPrecisionScaleText(Number precision, Number scale) {
        if (ObjectUtil.isNull(precision) && ObjectUtil.isNull(scale)) {
            return EMPTY;
        }
        return "精度：" + safeNumber(precision) + " / 小数位：" + safeNumber(scale);
    }

    public static String buildExtendedSummary(ColumnInfo columnInfo) {
        List<String> segments = CollUtil.newArrayList();
        if (ObjectUtil.isNotNull(columnInfo.getLength())) {
            segments.add("长度：" + columnInfo.getLength());
        }
        if (ObjectUtil.isNotNull(columnInfo.getPrecision()) || ObjectUtil.isNotNull(columnInfo.getScale())) {
            segments.add(buildPrecisionScaleText(columnInfo.getPrecision(), columnInfo.getScale()));
        }
        segments.add("自增：" + yesNo(columnInfo.getAutoIncrement()));
        segments.add("生成列：" + yesNo(columnInfo.getGenerated()));
        return StrUtil.join("；", segments);
    }
}
