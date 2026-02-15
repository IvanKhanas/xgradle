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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.processors.BomProcessor;
import org.altlinux.xgradle.interfaces.processors.BomResult;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.impl.model.ConfigurationInfoSnapshot;

import java.util.Map;
import java.util.Set;

/**
 * Applies BOM processing and propagates test context dependency markers.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ApplyBomsStep implements ResolutionStep {

    private final BomProcessor bomProcessor;

    @Inject
    ApplyBomsStep(BomProcessor bomProcessor) {
        this.bomProcessor = bomProcessor;
    }

    @Override
    public String name() {
        return "apply-boms";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        BomProcessor.Context bomContext =
                new BomProcessor.Context(
                        resolutionContext.getGradle(),
                        resolutionContext.getProjectDependencies()
                );

        BomResult bomResult = bomProcessor.process(bomContext);
        bomProcessor.removeBomsFromConfigurations(
                resolutionContext.getGradle(),
                bomResult.getProcessedBoms()
        );
        resolutionContext.setManagedVersions(bomResult.getManagedVersions());

        resolutionContext.getAllDependencies().clear();
        resolutionContext.getAllDependencies().addAll(bomContext.getProjectDependencies());

        ConfigurationInfoSnapshot snapshot = resolutionContext.getConfigurationInfoSnapshot();
        Map<String, Boolean> testFlags = snapshot != null
                ? snapshot.getTestDependencyFlags()
                : Map.of();
        Map<String, Set<String>> resolvedConfigNames = resolutionContext.getResolvedConfigNames();

        for (Map.Entry<String, Boolean> entry : testFlags.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                resolutionContext.getTestContextDependencies().add(entry.getKey());
            }
        }

        bomResult.getBomManagedDeps().forEach((bomKey, deps) -> {
            String[] parts = bomKey.split(":");
            if (parts.length < 2) {
                return;
            }

            String bomId = parts[0] + ":" + parts[1];
            Set<String> bomConfigs = resolvedConfigNames.get(bomId);
            if (!testFlags.getOrDefault(bomId, false)) {
                if (bomConfigs == null || bomConfigs.isEmpty()) {
                    return;
                }
            }

            for (String dep : deps) {
                String[] d = dep.split(":");
                if (d.length >= 2) {
                    String depKey = d[0] + ":" + d[1];
                    if (testFlags.getOrDefault(bomId, false)) {
                        resolutionContext.getTestContextDependencies().add(depKey);
                    }
                    if (bomConfigs != null && !bomConfigs.isEmpty()) {
                        resolvedConfigNames
                                .computeIfAbsent(depKey, k -> new java.util.HashSet<>())
                                .addAll(bomConfigs);
                    }
                }
            }
        });
    }
}
