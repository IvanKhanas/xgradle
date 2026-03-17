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
package unittests.resolution;

import org.altlinux.xgradle.interfaces.resolution.ResolvedArtifactsRegistry;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("ResolvedArtifactsRegistry contract")
class ResolvedArtifactsRegistryTests {

    @Test
    @DisplayName("Returns null when registry is absent")
    void returnsNullWhenRegistryAbsent() {
        Project project = ProjectBuilder.builder().withName("root").build();
        assertNull(ResolvedArtifactsRegistry.get(project));
    }

    @Test
    @DisplayName("Creates and reuses registry set")
    void createsAndReusesRegistrySet() {
        Project project = ProjectBuilder.builder().withName("root").build();

        Set<File> first = ResolvedArtifactsRegistry.getOrCreate(project);
        first.add(new File("lib-a.jar"));
        Set<File> second = ResolvedArtifactsRegistry.getOrCreate(project);

        assertAll(
                () -> assertSame(first, second),
                () -> assertTrue(second.contains(new File("lib-a.jar")))
        );
    }

    @Test
    @DisplayName("Replaces non-set extra property with registry set")
    void replacesNonSetExtraProperty() {
        Project project = ProjectBuilder.builder().withName("root").build();
        project.getExtensions().getExtraProperties().set(
                ResolvedArtifactsRegistry.EXTENSION_NAME,
                "not-a-set"
        );

        Set<File> created = ResolvedArtifactsRegistry.getOrCreate(project);

        assertAll(
                () -> assertNotNull(created),
                () -> assertTrue(project.getExtensions().getExtraProperties()
                        .get(ResolvedArtifactsRegistry.EXTENSION_NAME) instanceof Set)
        );
    }

    @Test
    @DisplayName("Gradle overload delegates to root project")
    void gradleOverloadDelegatesToRootProject() {
        Project project = ProjectBuilder.builder().withName("root").build();
        Gradle gradle = mock(Gradle.class);
        when(gradle.getRootProject()).thenReturn(project);

        Set<File> viaGradle = ResolvedArtifactsRegistry.getOrCreate(gradle);
        Set<File> viaProject = ResolvedArtifactsRegistry.getOrCreate(project);

        assertSame(viaProject, viaGradle);
    }
}
