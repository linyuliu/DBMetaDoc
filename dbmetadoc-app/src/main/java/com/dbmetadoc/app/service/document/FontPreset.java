package com.dbmetadoc.app.service.document;

import java.util.List;

/**
 * 字体预设定义。
 *
 * @author mumu
 * @date 2026-03-30
 */
public enum FontPreset {

    MODERN_CN("modern-cn", "现代中文", List.of("Source Han Sans CN", "Microsoft YaHei", "微软雅黑"),
            List.of("DengXian", "等线", "Microsoft YaHei", "微软雅黑"),
            List.of("Cascadia Mono", "JetBrains Mono", "LXGW WenKai Mono Screen", "Consolas")),
    CLASSIC_CN("classic-cn", "经典中文", List.of("SimHei", "黑体", "Microsoft YaHei", "微软雅黑"),
            List.of("SimSun", "宋体", "Microsoft YaHei", "微软雅黑"),
            List.of("Cascadia Mono", "SimSun", "宋体", "Consolas")),
    OFFICE_CN("office-cn", "办公中文", List.of("Microsoft YaHei", "微软雅黑", "Source Han Sans CN"),
            List.of("DengXian", "等线", "Microsoft YaHei", "微软雅黑"),
            List.of("JetBrains Mono", "Cascadia Mono", "Consolas"));

    private final String code;
    private final String label;
    private final List<String> titleCandidates;
    private final List<String> bodyCandidates;
    private final List<String> monoCandidates;

    FontPreset(String code, String label, List<String> titleCandidates, List<String> bodyCandidates, List<String> monoCandidates) {
        this.code = code;
        this.label = label;
        this.titleCandidates = titleCandidates;
        this.bodyCandidates = bodyCandidates;
        this.monoCandidates = monoCandidates;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public List<String> getTitleCandidates() {
        return titleCandidates;
    }

    public List<String> getBodyCandidates() {
        return bodyCandidates;
    }

    public List<String> getMonoCandidates() {
        return monoCandidates;
    }

    public static FontPreset fromCode(String code) {
        if (code != null) {
            for (FontPreset preset : values()) {
                if (preset.code.equalsIgnoreCase(code)) {
                    return preset;
                }
            }
        }
        return MODERN_CN;
    }
}


