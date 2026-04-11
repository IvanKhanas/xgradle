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
package org.altlinux.xgradle.impl.resolution;

import org.altlinux.xgradle.impl.model.ConfigurationInfoSnapshot;
import org.altlinux.xgradle.interfaces.processors.BomProcessor;
import org.altlinux.xgradle.interfaces.processors.BomResult;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApplyBomsStep")
class ApplyBomsStepTests {

    @Mock
    private BomProcessor bomProcessor;

    @Mock
    private Gradle gradle;

    @Test
    @DisplayName("Clears allDependencies and sets managedVersions from empty BOM result")
    void emptyBomResultClearsDepsAndSetsNoVersions() {
        when(bomProcessor.process(any())).thenReturn(BomResult.empty());

        ResolutionContext context = new ResolutionContext(gradle);
        context.getProjectDependencies().add("g:a");
        context.getAllDependencies().add("stale:entry");

        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        assertAll(
                () -> assertTrue(context.getManagedVersions().isEmpty()),
                () -> assertTrue(context.getAllDependencies().contains("g:a")),
                () -> assertFalse(context.getAllDependencies().contains("stale:entry"))
        );
        verify(bomProcessor).removeBomsFromConfigurations(gradle, Set.of());
    }

    @Test
    @DisplayName("Populates managedVersions from BOM result")
    void populatesManagedVersionsFromBomResult() {
        BomResult result = new BomResult(
                Map.of(),
                Map.of("g:a", "2.0"),
                Set.of()
        );
        when(bomProcessor.process(any())).thenReturn(result);

        ResolutionContext context = new ResolutionContext(gradle);
        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        assertEquals("2.0", context.getManagedVersions().get("g:a"));
    }

    @Test
    @DisplayName("Null snapshot leaves testContextDependencies empty")
    void nullSnapshotLeavesTestContextEmpty() {
        when(bomProcessor.process(any())).thenReturn(BomResult.empty());

        ResolutionContext context = new ResolutionContext(gradle);
        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        assertTrue(context.getTestContextDependencies().isEmpty());
    }

    @Test
    @DisplayName("Test-flagged dependencies from snapshot are added to testContextDependencies")
    void testFlaggedDepsFromSnapshotAddedToTestContext() {
        when(bomProcessor.process(any())).thenReturn(BomResult.empty());

        ConfigurationInfoSnapshot snapshot = new ConfigurationInfoSnapshot(
                Map.of(),
                Map.of("g:a", true, "g:b", false),
                Map.of()
        );
        ResolutionContext context = new ResolutionContext(gradle);
        context.setConfigurationInfoSnapshot(snapshot);

        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        assertAll(
                () -> assertTrue(context.getTestContextDependencies().contains("g:a")),
                () -> assertFalse(context.getTestContextDependencies().contains("g:b"))
        );
    }

    @Test
    @DisplayName("BOM key with fewer than two segments is skipped entirely")
    void malformedBomKeySkipped() {
        BomResult result = new BomResult(
                Map.of("onlyone", List.of("g:dep:1.0")),
                Map.of(),
                Set.of()
        );
        when(bomProcessor.process(any())).thenReturn(result);

        ResolutionContext context = new ResolutionContext(gradle);
        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        assertTrue(context.getResolvedConfigNames().isEmpty());
        assertTrue(context.getTestContextDependencies().isEmpty());
    }

    @Test
    @DisplayName("BOM not in testFlags and no bomConfigs skips its managed deps")
    void bomWithNoTestFlagAndNoConfigsSkipsItsDeps() {
        BomResult result = new BomResult(
                Map.of("g:bom:1.0", List.of("g:dep:1.0")),
                Map.of(),
                Set.of()
        );
        when(bomProcessor.process(any())).thenReturn(result);

        ResolutionContext context = new ResolutionContext(gradle);
        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        assertFalse(context.getResolvedConfigNames().containsKey("g:dep"));
    }

    @Test
    @DisplayName("BOM in testFlags propagates its managed deps to testContextDependencies")
    void bomInTestFlagsPropagatesDepsToTestContext() {
        BomResult result = new BomResult(
                Map.of("g:bom:1.0", List.of("g:dep:1.0")),
                Map.of(),
                Set.of()
        );
        when(bomProcessor.process(any())).thenReturn(result);

        ConfigurationInfoSnapshot snapshot = new ConfigurationInfoSnapshot(
                Map.of(),
                Map.of("g:bom", true),
                Map.of()
        );
        ResolutionContext context = new ResolutionContext(gradle);
        context.setConfigurationInfoSnapshot(snapshot);

        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        assertTrue(context.getTestContextDependencies().contains("g:dep"));
    }

    @Test
    @DisplayName("BOM with matching bomConfigs propagates config names to its managed deps")
    void bomConfigsPropagatesToManagedDeps() {
        BomResult result = new BomResult(
                Map.of("g:bom:1.0", List.of("g:dep:1.0")),
                Map.of(),
                Set.of()
        );
        when(bomProcessor.process(any())).thenReturn(result);

        Map<String, Set<String>> resolvedConfigNames = new HashMap<>();
        resolvedConfigNames.put("g:bom", Set.of("implementation"));

        ResolutionContext context = new ResolutionContext(gradle);
        context.getResolvedConfigNames().putAll(resolvedConfigNames);

        ApplyBomsStep step = new ApplyBomsStep(bomProcessor);
        step.execute(context);

        Set<String> depConfigs = context.getResolvedConfigNames().get("g:dep");
        assertNotNull(depConfigs);
        assertTrue(depConfigs.contains("implementation"));
    }

    @Test
    @DisplayName("name returns apply-boms")
    void nameReturnsApplyBoms() {
        assertEquals("apply-boms", new ApplyBomsStep(bomProcessor).name());
    }
}
