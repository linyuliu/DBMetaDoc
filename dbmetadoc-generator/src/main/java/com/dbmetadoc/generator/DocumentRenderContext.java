package com.dbmetadoc.generator;

import cn.hutool.core.collection.CollUtil;
import com.dbmetadoc.common.model.DatabaseInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 文档渲染上下文。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRenderContext {

    private String title;
    private DatabaseInfo database;
    private Set<String> visibleSections;
    private FontRenderProfile fontProfile;

    public boolean hasSection(String sectionCode) {
        return CollUtil.isNotEmpty(visibleSections) && visibleSections.contains(sectionCode);
    }

    public void setVisibleSections(Set<String> visibleSections) {
        this.visibleSections = visibleSections == null ? null : new LinkedHashSet<>(visibleSections);
    }
}
