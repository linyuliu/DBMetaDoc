package com.dbmetadoc.app.service;

import com.dbmetadoc.app.properties.FontProfileProperties;
import com.dbmetadoc.app.service.document.ResolvedFontProfile;
import com.dbmetadoc.generator.support.BundledFontSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FontProfileServiceTest {

    @Test
    void shouldResolveBundledOpenFontFallbacks() {
        FontProfileService service = new FontProfileService(new FontProfileProperties());

        ResolvedFontProfile profile = service.resolve("modern-cn",
                buildFontMap(BundledFontSupport.resolveBundledFontFiles()));

        assertEquals("Source Han Sans CN", profile.getTitleFont());
        assertEquals("Source Han Sans CN", profile.getBodyFont());
        assertEquals("JetBrains Mono", profile.getMonoFont());
        assertEquals("Noto Sans SC", profile.getSymbolFont());
        assertTrue(profile.getPdfFontResources().stream()
                .map(resource -> resource.getFamily())
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(Set.of("DBMetaDocPdfTitle", "DBMetaDocPdfBody", "DBMetaDocPdfMono", "DBMetaDocPdfSymbol")));
    }

    @Test
    void shouldScanConfiguredAdditionalDirectories(@TempDir Path tempDir) throws IOException {
        Path harmonyFont = Files.createFile(tempDir.resolve("HarmonyOS_Sans_SC_Regular.ttf"));
        FontProfileProperties properties = new FontProfileProperties();
        properties.setAdditionalDirectories(List.of(tempDir.toString()));
        FontProfileService service = new FontProfileService(properties);

        Map<String, Path> scanned = service.scanFontFiles();

        assertTrue(scanned.containsValue(harmonyFont));
    }

    @Test
    void shouldPreferBrandFontsWhenOptionalLocalFilesAreAvailable(@TempDir Path tempDir) throws IOException {
        FontProfileService service = new FontProfileService(new FontProfileProperties());
        Path sfDisplay = Files.createFile(tempDir.resolve("SF-Pro-Display-Regular.otf"));
        Path sfText = Files.createFile(tempDir.resolve("SF-Pro-Text-Regular.otf"));
        Path sfMono = Files.createFile(tempDir.resolve("SF-Mono-Regular.otf"));
        Path harmony = Files.createFile(tempDir.resolve("HarmonyOS_Sans_SC_Regular.ttf"));
        Path miSans = Files.createFile(tempDir.resolve("MiSans-Regular.ttf"));
        List<Path> fontPaths = new ArrayList<>(BundledFontSupport.resolveBundledFontFiles());
        fontPaths.addAll(List.of(sfDisplay, sfText, sfMono, harmony, miSans));

        ResolvedFontProfile profile = service.resolve("modern-cn", buildFontMap(fontPaths));

        assertEquals("SF Pro Display", profile.getTitleFont());
        assertEquals("SF Pro Text", profile.getBodyFont());
        assertEquals("JetBrains Mono", profile.getMonoFont());
        assertTrue(profile.getTitleFontCss().contains("\"Source Han Sans CN\""));
        assertTrue(profile.getBodyFontCss().contains("\"HarmonyOS Sans SC\""));
        assertTrue(profile.getBodyFontCss().contains("\"MiSans\""));
    }

    private Map<String, Path> buildFontMap(List<Path> paths) {
        return buildFontMap(paths.toArray(Path[]::new));
    }

    private Map<String, Path> buildFontMap(Path... paths) {
        Map<String, Path> fontFiles = new LinkedHashMap<>();
        for (Path path : paths) {
            fontFiles.put(normalizeFontFileName(path.getFileName().toString()), path);
        }
        return fontFiles;
    }

    private String normalizeFontFileName(String fileName) {
        return fileName
                .replaceAll("\\.(ttf|ttc|otf)$", "")
                .replaceAll("[\\s\\-_]", "")
                .toLowerCase();
    }
}
