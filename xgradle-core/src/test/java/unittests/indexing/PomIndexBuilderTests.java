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
package unittests.indexing;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

import org.altlinux.xgradle.interfaces.indexing.PomIndex;
import org.altlinux.xgradle.interfaces.indexing.PomIndexBuilder;
import org.altlinux.xgradle.impl.indexing.IndexingModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PomIndexBuilder contract")
class PomIndexBuilderTests {

    @Mock
    private PomIndex pomIndex;

    private PomIndexBuilder pomIndexBuilder;

    @BeforeEach
    void setUp() {
        Injector injector = Guice.createInjector(
                Modules.override(new IndexingModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(PomIndex.class).toInstance(pomIndex);
                    }
                })
        );

        pomIndexBuilder = injector.getInstance(PomIndexBuilder.class);
    }

    @Test
    @DisplayName("Delegates build to PomIndex and returns same instance")
    void delegatesBuildToPomIndex() {
        List<Path> poms = List.of(Path.of("/repo/a.pom"));

        PomIndex result = pomIndexBuilder.build(poms);

        verify(pomIndex).build(poms);
        assertSame(pomIndex, result);
    }
}
