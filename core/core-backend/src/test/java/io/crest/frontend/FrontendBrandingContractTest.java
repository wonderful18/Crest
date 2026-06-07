package io.crest.frontend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendBrandingContractTest {

    @Test
    void frontendShouldUseCrestTokenHeaderSuggestion() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/visualized/data/datasource/form/ApiHttpRequestForm.vue",
                List.of("X-CREST-TOKEN"), List.of("X-DE-TOKEN"));
    }

    @Test
    void customFontStyleIdsShouldUseCrestPrefix() throws IOException {
        assertFrontendContract("core/core-frontend/src/store/modules/appearance.ts",
                List.of("crest-custom_font"), List.of("de-custom_font"));
        assertFrontendContract("core/core-frontend/src/views/system/font/index.vue",
                List.of("crest-custom_font"), List.of("de-custom_font"));
    }

    @Test
    void embeddedToolbarEventsShouldUseCrestNames() throws IOException {
        assertFrontendContract("core/core-frontend/src/components/dashboard/DbToolbar.vue",
                List.of("crest-embedded-host", "crest-dashboard-editor-back"),
                List.of("dataease-embedded-host", "de-dashboard-editor-back"));
        assertFrontendContract("core/core-frontend/src/components/data-visualization/DvToolbar.vue",
                List.of("crest-embedded-host", "crest-dashboard-editor-back"),
                List.of("dataease-embedded-host", "de-dashboard-editor-back"));
    }

    @Test
    void frontendSourcesShouldNotUseLegacyBrandingContracts() throws IOException {
        assertFrontendTreeDoesNotContain(List.of(
                "--de-custom_font",
                "X-DE-TOKEN",
                "dataease-embedded-host",
                "de-dashboard-editor-back"
        ));
    }

    @Test
    void standaloneEngineSettingsShouldNotUseCommercialPackageNaming() throws IOException {
        assertFrontendContract("core/core-frontend/src/views/system/parameter/engine/EngineInfoTemplate.vue",
                List.of("getEngineDatasource"), legacyCommercialPackageTokens());
    }

    private void assertFrontendContract(
            String relativePath,
            List<String> requiredTokens,
            List<String> forbiddenTokens
    ) throws IOException {
        String source = Files.readString(repoRoot().resolve(relativePath));
        requiredTokens.forEach(token -> assertTrue(source.contains(token),
                relativePath + " should contain " + token));
        forbiddenTokens.forEach(token -> assertFalse(source.contains(token),
                relativePath + " should not contain " + token));
    }

    private void assertFrontendTreeDoesNotContain(List<String> forbiddenTokens) throws IOException {
        Path frontendRoot = repoRoot().resolve("core/core-frontend/src");
        try (Stream<Path> paths = Files.walk(frontendRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains("/node_modules/"))
                    .filter(this::isFrontendSourceFile)
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            forbiddenTokens.forEach(token -> assertFalse(source.contains(token),
                                    frontendRoot.relativize(path) + " should not contain " + token));
                        } catch (IOException e) {
                            throw new AssertionError("Unable to read " + path, e);
                        }
                    });
        }
    }

    private boolean isFrontendSourceFile(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".vue")
                || name.endsWith(".ts")
                || name.endsWith(".tsx")
                || name.endsWith(".js")
                || name.endsWith(".jsx")
                || name.endsWith(".mjs")
                || name.endsWith(".cjs")
                || name.endsWith(".json")
                || name.endsWith(".css")
                || name.endsWith(".less")
                || name.endsWith(".scss")
                || name.endsWith(".sass")
                || name.endsWith(".html")
                || name.endsWith(".md")
                || name.endsWith(".svg")
                || name.endsWith(".yaml")
                || name.endsWith(".yml");
    }

    private List<String> legacyCommercialPackageTokens() {
        String compactName = "x" + "Pack";
        String displayName = "X" + "-" + "Pack";
        return List.of(compactName, displayName, displayName.toLowerCase());
    }

    private Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("core/core-frontend/src/api/dataset.ts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate Crest repository root");
    }
}
