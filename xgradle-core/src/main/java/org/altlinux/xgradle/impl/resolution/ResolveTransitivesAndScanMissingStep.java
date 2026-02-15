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
import org.altlinux.xgradle.interfaces.processors.TransitiveProcessor;
import org.altlinux.xgradle.interfaces.processors.TransitiveResult;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.logging.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Expands dependency set using transitive traversal and ensures that all discovered coordinates are resolved against system libraries via VersionScanner.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class ResolveTransitivesAndScanMissingStep implements ResolutionStep {

    private final TransitiveProcessor transitiveProcessor;
    private final VersionScanner versionScanner;

    @Inject
    ResolveTransitivesAndScanMissingStep(
            TransitiveProcessor transitiveProcessor,
            VersionScanner versionScanner
    ) {
        this.transitiveProcessor = transitiveProcessor;
        this.versionScanner = versionScanner;
    }

    @Override
    public String name() {
        return "resolve-transitive-dependencies";
    }

    @Override
    public void execute(ResolutionContext resolutionContext) {
        TransitiveResult result = transitiveProcessor.process(
                resolutionContext.getSystemArtifacts(),
                resolutionContext.getTestContextDependencies(),
                resolutionContext.getDependencyScopes(),
                resolutionContext.getResolvedConfigNames()
        );

        Set<String> mainDependencyKeys = new HashSet<>(result.getMainDependencies());
        Set<String> testDependencyKeys = new HashSet<>(result.getTestDependencies());

        resolutionContext.getTestContextDependencies().addAll(testDependencyKeys);

        resolutionContext.getSkipped().clear();
        resolutionContext.getSkipped().addAll(result.getSkippedDependencies());

        Logger logger = resolutionContext.getGradle().getRootProject().getLogger();
        Set<String> beforeScan = new HashSet<>(resolutionContext.getSystemArtifacts().keySet());

        Map<String, MavenCoordinate> resolvedMain = versionScanner.scanSystemArtifacts(mainDependencyKeys);
        Map<String, MavenCoordinate> resolvedTest = versionScanner.scanSystemArtifacts(testDependencyKeys);

        for (Map.Entry<String, MavenCoordinate> entry : resolvedTest.entrySet()) {
            MavenCoordinate coordinate = entry.getValue();
            if (coordinate == null) {
                continue;
            }

            resolvedTest.put(
                    entry.getKey(),
                    coordinate.toBuilder()
                            .testContext(true)
                            .build()
            );
        }


        resolutionContext.getSystemArtifacts().putAll(resolvedMain);
        resolutionContext.getSystemArtifacts().putAll(resolvedTest);

        if (!beforeScan.isEmpty()) {
            Set<String> afterScan = new HashSet<>(resolvedMain.keySet());
            afterScan.addAll(resolvedTest.keySet());
            Set<String> dropped = new HashSet<>(beforeScan);
            dropped.removeAll(afterScan);
            if (!dropped.isEmpty()) {
                logger.lifecycle("Dropped {} artifacts after rescanning: {}", dropped.size(), dropped);
            }
        }
    }
}
