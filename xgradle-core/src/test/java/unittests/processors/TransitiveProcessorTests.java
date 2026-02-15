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
package unittests.processors;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.processors.ProcessorsModule;
import org.altlinux.xgradle.interfaces.managers.TransitiveDependencyManager;
import org.altlinux.xgradle.interfaces.processors.BomProcessor;
import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.altlinux.xgradle.interfaces.processors.TransitiveProcessor;
import org.altlinux.xgradle.interfaces.processors.TransitiveResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransitiveProcessor contract")
class TransitiveProcessorTests {

    @Mock
    private TransitiveDependencyManager manager;

    @Mock
    private PluginProcessor pluginProcessor;

    @Mock
    private BomProcessor bomProcessor;

    @Test
    @DisplayName("Marks test context dependencies and splits main/test sets")
    void marksTestContextDependencies() {
        when(manager.configure(any(), any(), any())).thenReturn(Set.of("skipped"));

        Injector injector = Guice.createInjector(
                Modules.override(new ProcessorsModule()).with(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TransitiveDependencyManager.class).toInstance(manager);
                        bind(PluginProcessor.class).toInstance(pluginProcessor);
                        bind(BomProcessor.class).toInstance(bomProcessor);
                    }
                })
        );

        TransitiveProcessor processor = injector.getInstance(TransitiveProcessor.class);

        MavenCoordinate a = MavenCoordinate.builder().groupId("g").artifactId("a").version("1").build();
        MavenCoordinate b = MavenCoordinate.builder().groupId("g").artifactId("b").version("1").testContext(true).build();

        Map<String, MavenCoordinate> systemArtifacts = new HashMap<>();
        systemArtifacts.put("g:a", a);
        systemArtifacts.put("g:b", b);

        TransitiveResult result = processor.process(
                systemArtifacts,
                Set.of("g:a"),
                new HashMap<>(),
                new HashMap<>()
        );

        assertTrue(systemArtifacts.get("g:a").isTestContext());
        assertTrue(result.getTestDependencies().containsAll(Set.of("g:a", "g:b")));
        assertTrue(result.getMainDependencies().isEmpty());
        assertEquals(Set.of("skipped"), result.getSkippedDependencies());
    }
}
