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
package unittests.redactors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.redactors.PomRedactor;
import org.altlinux.xgradle.impl.redactors.RedactorsModule;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.slf4j.Logger;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static unittests.PomXmlBuilder.*;

@DisplayName("PomRedactor")
class PomRedactorTests {

    @TempDir
    Path tempDir;

    private PomRedactor redactor;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);

        Injector injector = Guice.createInjector(
                Modules.override(new RedactorsModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Logger.class).toInstance(logger);
                            }
                        })
        );

        redactor = injector.getInstance(PomRedactor.class);
    }

    @Test
    @DisplayName("Is created by Guice")
    void isCreatedByGuice() {
        assertNotNull(redactor);
    }

    @Test
    @DisplayName("Adds dependency into <dependencies> when absent")
    void addsDependencyToDependencies() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .dependenciesBlock()
                .writeTo(tempDir.resolve("a.pom"));

        redactor.addDependency(pom, "org.a:lib:1.0:compile");

        Model model = readModel(pom);
        assertEquals(1, model.getDependencies().size());
        Dependency d = model.getDependencies().get(0);

        assertEquals("org.a", d.getGroupId());
        assertEquals("lib", d.getArtifactId());
        assertEquals("1.0", d.getVersion());
        assertEquals("compile", d.getScope());
    }

    @Test
    @DisplayName("Does not duplicate dependency when same groupId:artifactId already exists")
    void doesNotDuplicateByGroupArtifact() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .dep("org.a", "lib", "1.0", "compile")
                .writeTo(tempDir.resolve("dup.pom"));

        redactor.addDependency(pom, "org.a:lib:2.0:test");

        Model model = readModel(pom);
        assertEquals(1, model.getDependencies().size());
        Dependency d = model.getDependencies().get(0);

        assertEquals("org.a", d.getGroupId());
        assertEquals("lib", d.getArtifactId());
        assertEquals("1.0", d.getVersion());
        assertEquals("compile", d.getScope());
    }

    @Test
    @DisplayName("Also adds dependency into <dependencyManagement> when the block exists")
    void addsDependencyToDependencyManagementWhenExists() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .dependenciesBlock()
                .dependencyManagementBlock()
                .writeTo(tempDir.resolve("dm.pom"));

        redactor.addDependency(pom, "org.a:lib:1.0:compile");

        Model model = readModel(pom);
        assertNotNull(model.getDependencyManagement());
        assertEquals(1, model.getDependencyManagement().getDependencies().size());
    }

    @Test
    @DisplayName("Removes dependency from <dependencies> by groupId:artifactId")
    void removesFromDependencies() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .dep("org.a", "lib", "1.0", "compile")
                .dep("org.b", "x", "2.0", null)
                .writeTo(tempDir.resolve("rm.pom"));

        redactor.removeDependency(pom, "org.a:lib");

        Model model = readModel(pom);
        assertEquals(1, model.getDependencies().size());
        assertEquals("org.b", model.getDependencies().get(0).getGroupId());
    }

    @Test
    @DisplayName("Removes dependency from <dependencies> respecting optional scope filter")
    void removesRespectsScopeFilter() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .dep("org.a", "lib", "1.0", "compile")
                .dep("org.a", "lib", "1.0", "test")
                .writeTo(tempDir.resolve("scope.pom"));

        redactor.removeDependency(pom, "org.a:lib:1.0:test");

        Model model = readModel(pom);
        assertEquals(1, model.getDependencies().size());
        assertEquals("compile", model.getDependencies().get(0).getScope());
    }

    @Test
    @DisplayName("Removes dependency from <dependencyManagement> and nulls the block when it becomes empty")
    void removesFromDependencyManagementAndNullsWhenEmpty() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .dependenciesBlock()
                .managedDep("org.a", "lib", "1.0", "compile")
                .writeTo(tempDir.resolve("dmrm.pom"));

        redactor.removeDependency(pom, "org.a:lib");

        Model model = readModel(pom);
        assertNull(model.getDependencyManagement());
    }

    @Test
    @DisplayName("Changes dependency in both <dependencies> and <dependencyManagement>")
    void changesInDependenciesAndDependencyManagement() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .dep("org.a", "lib", "1.0", "compile")
                .managedDep("org.a", "lib", "1.0", "compile")
                .writeTo(tempDir.resolve("chg.pom"));

        redactor.changeDependency(pom, "org.a:lib:1.0:compile", "org.a:lib:2.0:test");

        Model model = readModel(pom);

        Dependency d1 = model.getDependencies().get(0);
        assertEquals("2.0", d1.getVersion());
        assertEquals("test", d1.getScope());

        Dependency d2 = model.getDependencyManagement().getDependencies().get(0);
        assertEquals("2.0", d2.getVersion());
        assertEquals("test", d2.getScope());
    }

    @Test
    @DisplayName("Removes <parent> block when it exists")
    void removesParentWhenPresent() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .parent("p", "parent", "9")
                .writeTo(tempDir.resolve("parent.pom"));

        redactor.removeParent(pom);

        Model model = readModel(pom);
        assertNull(model.getParent());
    }

    @Test
    @DisplayName("Logs warning and does nothing when <parent> block is absent")
    void warnsWhenNoParent() {
        Path pom = pom()
                .groupId("t").artifactId("a").version("1")
                .writeTo(tempDir.resolve("noparent.pom"));

        redactor.removeParent(pom);

        verify(logger, times(1)).warn(startsWith("POM file hasn`t parent block, cannot remove: "));
        Model model = readModel(pom);
        assertNull(model.getParent());
    }
}
