package io.crest.frontend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendLegacyComponentContractTest {

    @Test
    void renamedVisualizationComponentsShouldKeepLegacyDataCompatibility() throws IOException {
        String legacyComponent = readFrontendSource("src/utils/legacyComponent.ts");
        Map<String, String> renamedComponents = Map.of(
                "DeDecoration", "Decoration",
                "DeFrame", "Frame",
                "DeScreen", "Screen",
                "DeStreamMedia", "StreamMedia",
                "DeTabs", "Tabs",
                "DeTimeClock", "TimeClock",
                "DeVideo", "Video"
        );

        renamedComponents.forEach((legacyName, currentName) -> {
            assertTrue(legacyComponent.contains(legacyName + ": '" + currentName + "'"),
                    "legacy component data should map " + legacyName + " to " + currentName);
            assertTrue(readFrontendSourceUnchecked("src/utils/components.ts")
                            .contains(legacyName + ": " + currentName),
                    "component registry should keep a fallback for " + legacyName);
        });
    }

    @Test
    void visualizationDataIngressShouldNormalizeLegacyComponentNames() throws IOException {
        assertTrue(readFrontendSource("src/utils/canvasUtils.ts")
                        .contains("normalizeLegacyComponentData(canvasDataResult)"),
                "loaded visualization data should normalize legacy component names");
        assertTrue(readFrontendSource("src/utils/canvasUtils.ts")
                        .contains("normalizeLegacyComponent(componentItem)"),
                "nested history adaptation should normalize each component before branching");
        assertTrue(readFrontendSource("src/store/modules/data-visualization/dvMain.ts")
                        .contains("this.componentData = normalizeLegacyComponentData(componentData)"),
                "store-level component data writes should normalize legacy component names");
    }

    private String readFrontendSource(String relativePath) throws IOException {
        return Files.readString(repoRoot().resolve("core/core-frontend").resolve(relativePath));
    }

    private String readFrontendSourceUnchecked(String relativePath) {
        try {
            return readFrontendSource(relativePath);
        } catch (IOException e) {
            throw new AssertionError("Unable to read " + relativePath, e);
        }
    }

    private Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("core/core-frontend/src/utils/components.ts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate Crest repository root");
    }
}
