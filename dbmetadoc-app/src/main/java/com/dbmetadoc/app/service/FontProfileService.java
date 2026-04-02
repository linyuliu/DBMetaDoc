package com.dbmetadoc.app.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.dbmetadoc.app.properties.FontProfileProperties;
import com.dbmetadoc.app.service.document.FontPreset;
import com.dbmetadoc.app.service.document.ResolvedFontProfile;
import com.dbmetadoc.common.vo.FontPresetResponse;
import com.dbmetadoc.generator.PdfFontResource;
import com.dbmetadoc.generator.support.BundledFontSupport;
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

    private static final String PDF_TITLE_FAMILY = "DBMetaDocPdfTitle";
    private static final String PDF_BODY_FAMILY = "DBMetaDocPdfBody";
    private static final String PDF_MONO_FAMILY = "DBMetaDocPdfMono";
    private static final String PDF_SYMBOL_FAMILY = "DBMetaDocPdfSymbol";
    private static final List<String> SYMBOL_FONT_CANDIDATES = List.of(
            "PingFang SC",
            "HarmonyOS Sans SC",
            "MiSans",
            "Noto Sans SC",
            "Source Han Sans CN",
            "Segoe UI Symbol",
            "Segoe UI Emoji",
            "Microsoft YaHei",
            "微软雅黑"
    );

    /**
     * 常见中英文字体名 → 文件名前缀映射。
     * key 为 normalizeFontName 后的结果，value 为文件名前缀（小写，无扩展名）。
     */
    private static final Map<String, String> FONT_NAME_ALIASES;
    private static final List<String> PDF_CHINESE_FALLBACKS = List.of(
            "PingFang SC",
            "HarmonyOS Sans SC",
            "MiSans",
            "Source Han Sans CN",
            "Microsoft YaHei",
            "微软雅黑",
            "Noto Sans SC",
            "Noto Serif SC",
            "DengXian",
            "等线",
            "SimHei",
            "黑体",
            "SimSun",
            "宋体"
    );

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("microsoftyahei", "msyh");
        m.put("微软雅黑", "msyh");
        m.put("dengxian", "deng");
        m.put("等线", "deng");
        m.put("consolas", "consola");
        m.put("sfprotext", "sfprotext");
        m.put("sfprodisplay", "sfprodisplay");
        m.put("sfmono", "sfmono");
        m.put("pingfangsc", "pingfang");
        m.put("pingfang", "pingfang");
        m.put("misans", "misans");
        m.put("harmonyossans", "harmonyossans");
        m.put("harmonyossanssc", "harmonyossanssc");
        m.put("harmonysanssc", "harmonysanssc");
        m.put("jetbrainsmono", "jetbrainsmono");
        m.put("notosanssc", "notosanssc");
        m.put("notoserifsc", "notoserifsc");
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
        String symbolFont = pickInstalledFontName(SYMBOL_FONT_CANDIDATES, fontFiles);
        List<String> pdfFonts = new ArrayList<>();
        collectPdfFonts(pdfFonts, fontFiles, titleFont);
        collectPdfFonts(pdfFonts, fontFiles, bodyFont);
        collectPdfFonts(pdfFonts, fontFiles, monoFont);
        collectPdfFonts(pdfFonts, fontFiles, symbolFont);
        PDF_CHINESE_FALLBACKS.forEach(fontName -> collectPdfFonts(pdfFonts, fontFiles, fontName));
        List<PdfFontResource> pdfFontResources = buildPdfFontResources(fontFiles, titleFont, bodyFont, monoFont, symbolFont);
        Set<String> pdfFamilies = pdfFontResources.stream()
                .map(PdfFontResource::getFamily)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        log.info("字体解析结果 [{}]：title={}，body={}，mono={}，symbol={}，PDF字体文件数={}，PDF嵌入别名={}",
                preset.getCode(), titleFont, bodyFont, monoFont, symbolFont, pdfFonts.size(), pdfFamilies);
        return ResolvedFontProfile.builder()
                .code(preset.getCode())
                .label(preset.getLabel())
                .titleFont(titleFont)
                .bodyFont(bodyFont)
                .monoFont(monoFont)
                .symbolFont(symbolFont)
                .titleFontCss(toCssFamily(preset.getTitleCandidates(), titleFont, "sans-serif"))
                .bodyFontCss(toCssFamily(preset.getBodyCandidates(), bodyFont, "sans-serif"))
                .monoFontCss(toCssFamily(preset.getMonoCandidates(), monoFont, "monospace"))
                .symbolFontCss(toCssFamily(SYMBOL_FONT_CANDIDATES, symbolFont, "sans-serif"))
                .pdfTitleFontCss(toPdfCssFamily(pdfFamilies,
                        List.of(PDF_TITLE_FAMILY, PDF_BODY_FAMILY),
                        List.of(titleFont, bodyFont),
                        "sans-serif"))
                .pdfBodyFontCss(toPdfCssFamily(pdfFamilies,
                        List.of(PDF_BODY_FAMILY, PDF_TITLE_FAMILY),
                        List.of(bodyFont, titleFont),
                        "sans-serif"))
                .pdfMonoFontCss(toPdfCssFamily(pdfFamilies,
                        List.of(PDF_MONO_FAMILY, PDF_BODY_FAMILY),
                        List.of(monoFont, bodyFont),
                        "monospace"))
                .pdfSymbolFontCss(toPdfCssFamily(pdfFamilies,
                        List.of(PDF_SYMBOL_FAMILY, PDF_BODY_FAMILY, PDF_TITLE_FAMILY),
                        List.of(symbolFont, bodyFont, titleFont),
                        "sans-serif"))
                .pdfFontFiles(pdfFonts)
                .pdfFontResources(pdfFontResources)
                .build();
    }

    Map<String, Path> scanFontFiles() {
        Map<String, Path> fontFiles = new LinkedHashMap<>();
        BundledFontSupport.resolveBundledFontFiles().forEach(path ->
                fontFiles.putIfAbsent(normalizeFontFileName(path.getFileName().toString()), path));
        Set<Path> directories = new LinkedHashSet<>();
        directories.add(Paths.get("C:\\Windows\\Fonts"));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (StrUtil.isNotBlank(localAppData)) {
            directories.add(Paths.get(localAppData, "Microsoft", "Windows", "Fonts"));
        }
        directories.add(Paths.get(System.getProperty("user.home"), "Library", "Fonts"));
        directories.add(Paths.get("/Library/Fonts"));
        directories.add(Paths.get("/System/Library/Fonts"));
        directories.add(Paths.get(System.getProperty("user.home"), ".fonts"));
        directories.add(Paths.get("/usr/share/fonts"));
        if (CollUtil.isNotEmpty(fontProfileProperties.getAdditionalDirectories())) {
            fontProfileProperties.getAdditionalDirectories().stream()
                    .filter(StrUtil::isNotBlank)
                    .map(Paths::get)
                    .forEach(directories::add);
        }
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
        log.debug("扫描到 {} 个字体文件（含打包字体）", fontFiles.size());
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
        Path embeddable = findEmbeddableFontPath(fontFiles, fontName);
        if (embeddable != null) {
            return embeddable;
        }
        return findAnyFontPath(fontFiles, fontName);
    }

    private Path findAnyFontPath(Map<String, Path> fontFiles, String fontName) {
        String normalized = normalizeFontName(fontName);
        for (Map.Entry<String, Path> entry : fontFiles.entrySet()) {
            if (entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }
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

    private Path findEmbeddableFontPath(Map<String, Path> fontFiles, String fontName) {
        String normalized = normalizeFontName(fontName);
        for (Map.Entry<String, Path> entry : fontFiles.entrySet()) {
            if (isEmbeddableFont(entry.getValue()) && entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }
        String alias = FONT_NAME_ALIASES.get(normalized);
        if (alias != null) {
            for (Map.Entry<String, Path> entry : fontFiles.entrySet()) {
                if (isEmbeddableFont(entry.getValue()) && entry.getKey().startsWith(alias)) {
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
        fonts.addAll(PDF_CHINESE_FALLBACKS);
        fonts.add(fallback);
        return fonts.stream()
                .map(this::quoteFontFamily)
                .reduce((left, right) -> left + ", " + right)
                .orElse(fallback);
    }

    private List<PdfFontResource> buildPdfFontResources(Map<String, Path> fontFiles,
                                                        String titleFont,
                                                        String bodyFont,
                                                        String monoFont,
                                                        String symbolFont) {
        List<PdfFontResource> resources = new ArrayList<>();
        addPdfFontResource(resources, PDF_TITLE_FAMILY,
                findFirstEmbeddable(fontFiles, titleFont, bodyFont));
        addPdfFontResource(resources, PDF_BODY_FAMILY,
                findFirstEmbeddable(fontFiles, bodyFont, titleFont));
        addPdfFontResource(resources, PDF_MONO_FAMILY,
                findFirstEmbeddable(fontFiles, monoFont, bodyFont, titleFont));
        addPdfFontResource(resources, PDF_SYMBOL_FAMILY,
                findFirstEmbeddable(fontFiles, symbolFont, bodyFont, titleFont));
        return resources;
    }

    private Path findFirstEmbeddable(Map<String, Path> fontFiles, String... preferredFonts) {
        for (String fontName : preferredFonts) {
            Path fontPath = findEmbeddableFontPath(fontFiles, fontName);
            if (fontPath != null) {
                return fontPath;
            }
        }
        for (String fallback : PDF_CHINESE_FALLBACKS) {
            Path fontPath = findEmbeddableFontPath(fontFiles, fallback);
            if (fontPath != null) {
                return fontPath;
            }
        }
        return null;
    }

    private void addPdfFontResource(List<PdfFontResource> resources, String family, Path fontPath) {
        if (fontPath == null) {
            return;
        }
        resources.add(PdfFontResource.builder()
                .family(family)
                .sourceUri(fontPath.toUri().toString())
                .build());
    }

    private String toPdfCssFamily(Set<String> availableAliases,
                                  List<String> preferredAliases,
                                  List<String> preferredFonts,
                                  String fallback) {
        LinkedHashSet<String> fonts = new LinkedHashSet<>();
        for (String alias : preferredAliases) {
            if (availableAliases.contains(alias)) {
                fonts.add(alias);
            }
        }
        fonts.addAll(preferredFonts);
        fonts.addAll(PDF_CHINESE_FALLBACKS);
        fonts.add(fallback);
        return fonts.stream()
                .filter(StrUtil::isNotBlank)
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

    private boolean isEmbeddableFont(Path path) {
        String value = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return value.endsWith(".ttf") || value.endsWith(".otf");
    }
}


