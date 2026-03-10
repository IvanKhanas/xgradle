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
package unittests.models;

import org.altlinux.xgradle.impl.enums.SbomComponentKind;
import org.altlinux.xgradle.impl.models.SbomComponent;
import org.altlinux.xgradle.impl.models.SbomLicense;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SBOM model contracts")
class SbomModelTests {

    @Test
    @DisplayName("Maven component exposes expected display name and unique key")
    void mavenComponentHasExpectedKeys() {
        SbomComponent component = SbomComponent.maven(
                "org.example",
                "demo-lib",
                "1.2.3"
        );

        assertEquals("org.example:demo-lib", component.displayName());
        assertEquals("org.example:demo-lib:1.2.3", component.uniqueKey());
        assertEquals(SbomComponentKind.LIBRARY, component.getComponentKind());
    }

    @Test
    @DisplayName("Plugin component keeps kind and metadata")
    void pluginComponentKeepsKindAndMetadata() {
        SbomLicense license = new SbomLicense("Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0");
        SbomComponent component = SbomComponent.mavenPlugin(
                "com.gradleup.shadow",
                "shadow-gradle-plugin",
                "8.3.8",
                "https://github.com/GradleUp/shadow",
                "https://github.com/GradleUp/shadow.git",
                List.of(license)
        );

        assertEquals(SbomComponentKind.GRADLE_PLUGIN, component.getComponentKind());
        assertEquals("https://github.com/GradleUp/shadow", component.getProjectUrl());
        assertEquals("https://github.com/GradleUp/shadow.git", component.getScmUrl());
        assertEquals(1, component.getLicenses().size());
    }

    @Test
    @DisplayName("File component uses file display name and unique key")
    void fileComponentUsesFileName() {
        SbomComponent component = SbomComponent.file("standalone.jar");

        assertEquals("standalone.jar", component.displayName());
        assertEquals("file:standalone.jar", component.uniqueKey());
        assertEquals(SbomComponentKind.FILE, component.getComponentKind());
        assertNull(component.getGroupId());
        assertNull(component.getArtifactId());
    }

    @Test
    @DisplayName("Falls back to unknown component identity when file name is missing")
    void fallsBackToUnknownFileIdentity() {
        SbomComponent component = SbomComponent.file(null);
        assertEquals("unknown", component.displayName());
        assertEquals("file:unknown", component.uniqueKey());
    }

    @Test
    @DisplayName("Versionless maven key ends with empty version segment")
    void versionlessMavenUniqueKeyContainsEmptyVersionSegment() {
        SbomComponent component = SbomComponent.maven("org.example", "demo-lib", null);
        assertEquals("org.example:demo-lib:", component.uniqueKey());
    }

    @Test
    @DisplayName("License values are normalized")
    void licenseValuesAreNormalized() {
        SbomLicense license = new SbomLicense(" Apache-2.0 ", "   ");

        assertEquals("Apache-2.0", license.getName());
        assertNull(license.getUrl());
    }

    @Test
    @DisplayName("Component keeps defensive copy of licenses list")
    void componentKeepsDefensiveCopyOfLicensesList() {
        List<SbomLicense> source = new ArrayList<>();
        source.add(new SbomLicense("Apache-2.0", null));

        SbomComponent component = SbomComponent.maven(
                "org.example",
                "demo-lib",
                "1.0.0",
                null,
                null,
                source
        );

        source.clear();
        assertEquals(1, component.getLicenses().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> component.getLicenses().add(new SbomLicense("MIT", null))
        );
    }
}
