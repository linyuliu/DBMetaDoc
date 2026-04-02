package com.dbmetadoc.generator.support;

import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 打包字体资源支持。
 *
 * <p>PDF 生成需要可嵌入的字体文件，资源在打成 jar 后不能直接当作本地文件路径使用，
 * 因此这里会把 classpath 中的字体临时落地到系统临时目录，并缓存路径供运行期复用。</p>
 *
 * @author mumu
 * @date 2026-04-02
 */
public final class BundledFontSupport {

    public static final String TITLE_RESOURCE = "/fonts/SourceHanSansCN-Bold.otf";
    public static final String BODY_RESOURCE = "/fonts/SourceHanSansCN-Regular.otf";
    public static final String MONO_RESOURCE = "/fonts/JetBrainsMono-Regular.ttf";
    public static final String MONO_BOLD_RESOURCE = "/fonts/JetBrainsMono-Bold.ttf";
    public static final String SYMBOL_RESOURCE = "/fonts/NotoSansSC-VF.ttf";

    private static final List<String> RESOURCE_PATHS = List.of(
            TITLE_RESOURCE,
            BODY_RESOURCE,
            MONO_RESOURCE,
            MONO_BOLD_RESOURCE,
            SYMBOL_RESOURCE
    );

    private static final Map<String, Path> CACHE = new ConcurrentHashMap<>();

    private BundledFontSupport() {
    }

    /**
     * 解析所有打包字体，并返回本地临时文件路径。
     */
    public static List<Path> resolveBundledFontFiles() {
        List<Path> paths = new ArrayList<>();
        for (String resourcePath : RESOURCE_PATHS) {
            Path path = resolveBundledFontFile(resourcePath);
            if (path != null) {
                paths.add(path);
            }
        }
        return paths;
    }

    /**
     * 解析单个打包字体。
     */
    public static Path resolveBundledFontFile(String resourcePath) {
        if (StrUtil.isBlank(resourcePath)) {
            return null;
        }
        return CACHE.computeIfAbsent(resourcePath, BundledFontSupport::extractFontToTemp);
    }

    private static Path extractFontToTemp(String resourcePath) {
        try (InputStream inputStream = BundledFontSupport.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            String fileName = Path.of(resourcePath).getFileName().toString();
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "dbmetadoc-fonts");
            Files.createDirectories(tempDir);
            Path tempFile = tempDir.resolve(fileName);
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            tempFile.toFile().deleteOnExit();
            tempDir.toFile().deleteOnExit();
            return tempFile;
        } catch (IOException ex) {
            throw new IllegalStateException("打包字体资源提取失败: " + resourcePath, ex);
        }
    }
}
