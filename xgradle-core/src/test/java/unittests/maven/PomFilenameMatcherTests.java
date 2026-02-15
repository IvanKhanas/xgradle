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
package unittests.maven;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.maven.MavenModule;
import org.altlinux.xgradle.interfaces.maven.PomFinder;
import org.altlinux.xgradle.interfaces.maven.PomFilenameMatcher;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PomFilenameMatcher contract")
class PomFilenameMatcherTests {

    @Mock
    private PomFinder pomFinder;

    @Mock
    private PomHierarchyLoader pomHierarchyLoader;

    @Test
    @DisplayName("Matches base and versioned POM names")
    void matchesBaseAndVersionedNames() {
        Injector injector = Guice.createInjector(
                Modules.override(new MavenModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomHierarchyLoader.class).toInstance(pomHierarchyLoader);
                    }
                })
        );
        PomFilenameMatcher matcher = injector.getInstance(PomFilenameMatcher.class);

        assertTrue(matcher.matches(Path.of("lib.pom"), "lib", "lib"));
        assertTrue(matcher.matches(Path.of("lib-1.2.3.pom"), "lib", "lib"));
        assertFalse(matcher.matches(Path.of("lib.jar"), "lib", "lib"));
    }

    @Test
    @DisplayName("Generates name variants from groupId")
    void generatesNameVariants() {
        Injector injector = Guice.createInjector(
                Modules.override(new MavenModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomFinder.class).toInstance(pomFinder);
                        bind(PomHierarchyLoader.class).toInstance(pomHierarchyLoader);
                    }
                })
        );
        PomFilenameMatcher matcher = injector.getInstance(PomFilenameMatcher.class);

        Set<String> variants = matcher.generateNameVariants("org.example.tools", "lib");
        assertTrue(variants.contains("lib"));
        assertTrue(variants.contains("example-tools-lib"));
        assertTrue(variants.contains("example-lib"));
    }
}
