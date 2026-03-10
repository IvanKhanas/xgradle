/*
 * Copyright 2025 BaseALT Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package unittests.builders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.altlinux.xgradle.impl.enums.SbomFormat;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.models.SbomLicense;
import org.altlinux.xgradle.interfaces.builders.SbomDocumentBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import unittests.AbstractSbomModuleTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SBOM document builders")
class SbomDocumentBuilderTests extends AbstractSbomModuleTest {

    @Test
    @DisplayName("Registers builders for all supported formats")
    void registersBuildersForAllSupportedFormats() {
        Map<SbomFormat, SbomDocumentBuilder> builders = buildersByFormat();
        assertEquals(2, builders.size());
        assertTrue(builders.containsKey(SbomFormat.SPDX));
        assertTrue(builders.containsKey(SbomFormat.CYCLONEDX));
    }

    @Test
    @DisplayName("SPDX builder emits normalized package fields")
    void spdxBuilderEmitsNormalizedPackageFields() {
        SbomDocumentBuilder builder = buildersByFormat().get(SbomFormat.SPDX);
        JsonObject document = builder.build(
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.maven(
                                "org.example",
                                "demo-lib",
                                null,
                                null,
                                "https://scm.example/repo.git",
                                List.of(
                                        new SbomLicense("Apache-2.0", null),
                                        new SbomLicense("not SPDX license", null)
                                )
                        ),
                        SbomComponent.file("standalone.jar")
                )
        );

        assertEquals("SPDX-2.3", document.get("spdxVersion").getAsString());
        JsonArray packages = document.getAsJsonArray("packages");
        assertEquals(2, packages.size());

        JsonObject first = packages.get(0).getAsJsonObject();
        assertEquals("org.example:demo-lib", first.get("name").getAsString());
        assertEquals("NOASSERTION", first.get("versionInfo").getAsString());
        assertEquals("Apache-2.0", first.get("licenseDeclared").getAsString());
        assertEquals("https://scm.example/repo.git", first.get("homepage").getAsString());
        assertFalse(first.has("downloadLocation"));

        JsonObject second = packages.get(1).getAsJsonObject();
        assertEquals("standalone.jar", second.get("name").getAsString());
        assertEquals("NOASSERTION", second.get("homepage").getAsString());
        assertEquals("NOASSERTION", second.get("licenseDeclared").getAsString());
        assertFalse(second.has("downloadLocation"));
    }

    @Test
    @DisplayName("SPDX builder joins valid SPDX licenses and ignores invalid identifiers")
    void spdxBuilderJoinsOnlyValidSpdxLicenseIdentifiers() {
        SbomDocumentBuilder builder = buildersByFormat().get(SbomFormat.SPDX);
        JsonObject document = builder.build(
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.maven(
                                "org.example",
                                "demo-lib",
                                "1.2.3",
                                null,
                                null,
                                List.of(
                                        new SbomLicense("Apache-2.0", null),
                                        new SbomLicense("MIT", null),
                                        new SbomLicense("GPL v2", null),
                                        new SbomLicense(" ", null)
                                )
                        )
                )
        );

        JsonObject component = document.getAsJsonArray("packages").get(0).getAsJsonObject();
        assertEquals("Apache-2.0 OR MIT", component.get("licenseDeclared").getAsString());
    }

    @Test
    @DisplayName("SPDX builder maps runtime names and URLs to SPDX identifiers")
    void spdxBuilderMapsRuntimeNamesAndUrlsToSpdxIdentifiers() {
        SbomDocumentBuilder builder = buildersByFormat().get(SbomFormat.SPDX);
        JsonObject document = builder.build(
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.maven(
                                "org.example",
                                "demo-lib",
                                "1.2.3",
                                null,
                                null,
                                List.of(
                                        new SbomLicense("The Apache Software License, Version 2.0", null),
                                        new SbomLicense(null, "https://opensource.org/licenses/0BSD"),
                                        new SbomLicense("Apache-2.0", null)
                                )
                        )
                )
        );

        JsonObject component = document.getAsJsonArray("packages").get(0).getAsJsonObject();
        assertEquals("Apache-2.0 OR 0BSD", component.get("licenseDeclared").getAsString());
    }

    @Test
    @DisplayName("SPDX builder prefers project URL over SCM URL for homepage")
    void spdxBuilderPrefersProjectUrlOverScmUrl() {
        SbomDocumentBuilder builder = buildersByFormat().get(SbomFormat.SPDX);
        JsonObject document = builder.build(
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.maven(
                                "org.example",
                                "demo-lib",
                                "1.2.3",
                                "https://project.example",
                                "https://scm.example/repo.git",
                                List.of()
                        )
                )
        );

        JsonObject component = document.getAsJsonArray("packages").get(0).getAsJsonObject();
        assertEquals("https://project.example", component.get("homepage").getAsString());
    }

    @Test
    @DisplayName("CycloneDX builder emits plugin metadata and references")
    void cycloneDxBuilderEmitsPluginMetadataAndReferences() {
        SbomDocumentBuilder builder = buildersByFormat().get(SbomFormat.CYCLONEDX);
        JsonObject document = builder.build(
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.mavenPlugin(
                                "com.gradleup.shadow",
                                "shadow-gradle-plugin",
                                "8.3.8",
                                "https://github.com/GradleUp/shadow",
                                "https://github.com/GradleUp/shadow.git",
                                List.of(new SbomLicense("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"))
                        ),
                        SbomComponent.file("bundle.jar")
                )
        );

        assertEquals("CycloneDX", document.get("bomFormat").getAsString());
        JsonArray components = document.getAsJsonArray("components");
        assertEquals(2, components.size());

        JsonObject plugin = findComponent(components, "com.gradleup.shadow", "shadow-gradle-plugin");
        assertNotNull(plugin);
        assertEquals("framework", plugin.get("type").getAsString());
        assertEquals("pkg:maven/com.gradleup.shadow/shadow-gradle-plugin@8.3.8", plugin.get("purl").getAsString());

        JsonArray properties = plugin.getAsJsonArray("properties");
        assertNotNull(properties);
        assertEquals("xgradle:component-kind", properties.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("gradle-plugin", properties.get(0).getAsJsonObject().get("value").getAsString());

        JsonArray refs = plugin.getAsJsonArray("externalReferences");
        assertNotNull(refs);
        assertTrue(refs.size() >= 2);

        JsonArray licenses = plugin.getAsJsonArray("licenses");
        assertNotNull(licenses);
        JsonObject license = licenses.get(0).getAsJsonObject().getAsJsonObject("license");
        assertEquals("Apache-2.0", license.get("name").getAsString());

        JsonObject file = findComponent(components, null, "bundle.jar");
        assertNotNull(file);
        assertEquals("file", file.get("type").getAsString());
        assertTrue(file.get("purl") == null || file.get("purl").isJsonNull());
        assertTrue(file.get("properties") == null || file.get("properties").isJsonNull());
    }

    @Test
    @DisplayName("CycloneDX builder skips empty license and reference payloads")
    void cycloneDxBuilderSkipsEmptyLicenseAndReferencePayloads() {
        SbomDocumentBuilder builder = buildersByFormat().get(SbomFormat.CYCLONEDX);
        JsonObject document = builder.build(
                "demo-project",
                "1.0.0",
                List.of(
                        SbomComponent.maven(
                                "org.example",
                                "demo-lib",
                                "1.2.3",
                                "  ",
                                null,
                                Arrays.asList(new SbomLicense(" ", " "))
                        )
                )
        );

        JsonObject component = document.getAsJsonArray("components").get(0).getAsJsonObject();
        assertTrue(component.get("externalReferences") == null || component.get("externalReferences").isJsonNull());
        assertTrue(component.get("licenses") == null || component.get("licenses").isJsonNull());
    }

    private Map<SbomFormat, SbomDocumentBuilder> buildersByFormat() {
        Set<SbomDocumentBuilder> builders = injector.getInstance(
                Key.get(new TypeLiteral<Set<SbomDocumentBuilder>>() {})
        );

        Map<SbomFormat, SbomDocumentBuilder> byFormat = new EnumMap<>(SbomFormat.class);
        for (SbomDocumentBuilder builder : builders) {
            byFormat.put(builder.format(), builder);
        }
        return byFormat;
    }

    private JsonObject findComponent(
            JsonArray components,
            String group,
            String name
    ) {
        for (JsonElement element : components) {
            JsonObject component = element.getAsJsonObject();
            JsonElement groupElement = component.get("group");
            JsonElement nameElement = component.get("name");
            if (nameElement == null || !name.equals(nameElement.getAsString())) {
                continue;
            }

            if (group == null && (groupElement == null || groupElement.isJsonNull())) {
                return component;
            }

            if (group != null
                    && groupElement != null
                    && !groupElement.isJsonNull()
                    && group.equals(groupElement.getAsString())) {
                return component;
            }
        }
        return null;
    }
}
