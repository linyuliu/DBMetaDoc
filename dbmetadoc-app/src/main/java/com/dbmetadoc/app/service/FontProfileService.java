package com.dbmetadoc.app.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.app.properties.FontProfileProperties;
import com.dbmetadoc.app.service.document.FontPreset;
import com.dbmetadoc.app.service.document.ResolvedFontProfile;
import com.dbmetadoc.common.vo.FontPresetResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 字体预设与本机字体探测服务。
 *
 * @author mumu
 * @date 2026-03-30
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FontProfileService {

    /**
     * 常见中英文字体名 → 文件名前缀映射。
     * key 为 normalizeFontName 后的结果，value 为文件名前缀（小写，无扩展名）。
     */
    private static final Map<String, String> FONT_NAME_ALIASES;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("microsoftyahei", "msyh");
        m.put("微软雅黑", "msyh");
        m.put("dengxian", "deng");
        m.put("等线", "deng");
        m.put("consolas", "consola");
        m.put("simsun", "simsun");
        m.put("宋体", "simsun");
        m.put("simhei", "simhei");
        m.put("黑体", "simhei");
        m.put("sourcehanscn", "sourcehanscn");
        m.put("sourcehansanscn", "sourcehansanscn");
        FONT_NAME_ALIASES = Map.copyOf(m);
    }

    private final FontProfileProperties fontProfileProperties;

    public List<FontPresetResponse> listOptions() {
        Map<String, Path> fontFiles = scanFontFiles();
        return java.util.Arrays.stream(FontPreset.values())
                .map(preset -> resolve(preset.getCode(), fontFiles))
                .map(profile -> FontPresetResponse.builder()
                        .code(profile.getCode())
                        .label(profile.getLabel())
                        .titleFont(profile.getTitleFont())
                        .bodyFont(profile.getBodyFont())
                        .monoFont(profile.getMonoFont())
                        .build())
                .toList();
    }

    public String defaultCode() {
        return FontPreset.MODERN_CN.getCode();
    }

    public ResolvedFontProfile resolve(String presetCode) {
        return resolve(presetCode, scanFontFiles());
    }

    ResolvedFontProfile resolve(String presetCode, Map<String, Path> fontFiles) {
        FontPreset preset = FontPreset.fromCode(presetCode);
        String titleFont = pickInstalledFontName(preset.getTitleCandidates(), fontFiles);
        String bodyFont = pickInstalledFontName(preset.getBodyCandidates(), fontFiles);
        String monoFont = pickInstalledFontName(preset.getMonoCandidates(), fontFiles);
        List<String> pdfFonts = new ArrayList<>();
        collectPdfFonts(pdfFonts, fontFiles, titleFont);
        collectPdfFonts(pdfFonts, fontFiles, bodyFont);
        collectPdfFonts(pdfFonts, fontFiles, monoFont);
        log.info("字体解析结果 [{}]：title={}，body={}，mono={}，PDF字体文件数={}",
                preset.getCode(), titleFont, bodyFont, monoFont, pdfFonts.size());
        return ResolvedFontProfile.builder()
                .code(preset.getCode())
                .label(preset.getLabel())
                .titleFont(titleFont)
                .bodyFont(bodyFont)
                .monoFont(monoFont)
                .titleFontCss(toCssFamily(preset.getTitleCandidates(), titleFont, "sans-serif"))
                .bodyFontCss(toCssFamily(preset.getBodyCandidates(), bodyFont, "sans-serif"))
                .monoFontCss(toCssFamily(preset.getMonoCandidates(), monoFont, "monospace"))
                .pdfFontFiles(pdfFonts)
                .build();
    }

    Map<String, Path> scanFontFiles() {
        Set<Path> directories = new LinkedHashSet<>();
        directories.add(Paths.get("C:\\Windows\\Fonts"));
        directories.add(Paths.get(System.getProperty("user.home"), ".fonts"));
        directories.add(Paths.get("/usr/share/fonts"));
        if (CollUtil.isNotEmpty(fontProfileProperties.getAdditionalDirectories())) {
            fontProfileProperties.getAdditionalDirectories().stream()
                    .filter(StrUtil::isNotBlank)
                    .map(Paths::get)
                    .forEach(directories::add);
        }
        Map<String, Path> fontFiles = new LinkedHashMap<>();
        for (Path directory : directories) {
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (var stream = Files.walk(directory)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                            return name.endsWith(".ttf") || name.endsWith(".ttc") || name.endsWith(".otf");
                        })
                        .forEach(path -> fontFiles.putIfAbsent(normalizeFontFileName(path.getFileName().toString()), path));
            } catch (Exception e) {
                log.debug("扫描字体目录失败，目录：{}，原因：{}", directory, e.getMessage());
            }
        }
        log.debug("扫描到 {} 个字体文件", fontFiles.size());
        return fontFiles;
    }

    private String pickInstalledFontName(List<String> candidates, Map<String, Path> fontFiles) {
        for (String candidate : candidates) {
            if (findFontPath(fontFiles, candidate) != null) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    /**
     * 收集某字体名对应的所有字体文件（含粗体/斜体等变体），用于 PDF 嵌入。
     */
    private void collectPdfFonts(List<String> targets, Map<String, Path> fontFiles, String fontName) {
        if (StrUtil.isBlank(fontName)) {
            return;
        }
        String normalized = normalizeFontName(fontName);
        String alias = FONT_NAME_ALIASES.get(normalized);
        for (Map.Entry<String, Path> entry : fontFiles.entrySet()) {
            boolean matches = entry.getKey().contains(normalized)
                    || (alias != null && entry.getKey().startsWith(alias));
            if (matches) {
                String resolved = entry.getValue().toAbsolutePath().toString();
                if (!targets.contains(resolved)) {
                    targets.add(resolved);
                }
            }
        }
    }

    private Path findFontPath(Map<String, Path> fontFiles, String fontName) {
        String normalized = normalizeFontName(fontName);
        // 直接匹配：文件名包含字体名
        for (Map.Entry<String, Path> entry : fontFiles.entrySet()) {
            if (entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }
        // 别名匹配：通过常见字体名→文件名映射
        String alias = FONT_NAME_ALIASES.get(normalized);
        if (alias != null) {
            for (Map.Entry<String, Path> entry : fontFiles.entrySet()) {
                if (entry.getKey().startsWith(alias)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String toCssFamily(List<String> candidates, String primary, String fallback) {
        LinkedHashSet<String> fonts = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(primary)) {
            fonts.add(primary);
        }
        fonts.addAll(candidates);
        fonts.add(fallback);
        return fonts.stream()
                .map(this::quoteFontFamily)
                .reduce((left, right) -> left + ", " + right)
                .orElse(fallback);
    }

    private String quoteFontFamily(String font) {
        if ("sans-serif".equals(font) || "serif".equals(font) || "monospace".equals(font)) {
            return font;
        }
        return "\"" + font + "\"";
    }

    private String normalizeFontFileName(String fileName) {
        return normalizeFontName(fileName.replaceAll("\\.(ttf|ttc|otf)$", ""));
    }

    private String normalizeFontName(String value) {
        return StrUtil.blankToDefault(value, "")
                .replaceAll("[\\s\\-_]", "")
                .toLowerCase(Locale.ROOT);
    }
}


