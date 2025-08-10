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
package org.altlinux.gradlePlugin.utils.loggers;

import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.utils.Painter;
import org.gradle.api.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.altlinux.gradlePlugin.utils.Painter.*;

public class DependencyLogger {
    public void logSection(String title, Logger logger) {
        logger.lifecycle(green("\n--- " + title + " ---"));
    }

    public void logInitialDependencies(Set<String> dependencies, Logger logger) {
        logger.lifecycle("Found {} dependencies", dependencies.size());
    }

    public void logBomDependencies(Map<String, List<String>> bomDeps, Logger logger) {
        bomDeps.forEach((bom, deps) ->
                logger.lifecycle("BOM: {} ({} dependencies)", cyan(bom), deps.size())
        );
    }

    public void logResolvedArtifacts(Map<String, MavenCoordinate> artifacts, Logger logger) {
        logger.lifecycle("Resolved {} artifacts", artifacts.size());
        logger.info("System artifacts:");
        artifacts.forEach((k, v) ->
                logger.info(" - {}:{}:{}", v.getGroupId(), v.getArtifactId(), v.getVersion())
        );
    }

    public void logNewDependencies(Set<String> newDeps, Logger logger) {
        logger.lifecycle("Found {} dependencies", newDeps.size());
        newDeps.forEach(dep -> logger.info(" - {}", dep));
    }

    public void logTestContextDependencies(Set<String> testDeps, Logger logger) {
        logger.lifecycle("Test context dependencies: {}", testDeps.size());
        testDeps.forEach(dep -> logger.info(" - {}", dep));
    }

    public void logConfigurationArtifacts(Map<String, Set<String>> artifacts, Logger logger) {
        artifacts.forEach((cfg, arts) -> {
            logger.lifecycle("Configuration '{}' ({} artifacts):", cyan(cfg), arts.size());
            arts.forEach(a -> logger.lifecycle(" - {}", a));
        });
    }

    public void logSkippedDependencies(Set<String> notFound, Set<String> skipped, Logger logger) {
        if (!notFound.isEmpty()) {
            logger.lifecycle("Not found dependencies:");
            notFound.forEach(d -> logger.lifecycle(" - {}", d));
        }

        if (!skipped.isEmpty()) {
            logger.lifecycle("Not found transitive dependencies:");
            skipped.forEach(d -> logger.lifecycle(" - {}", d));
        }
    }

    public void logSubstitutions(
            Map<String, String> overrideLogs,
            Map<String, String> applyLogs,
            Logger logger
    ) {
        if (!overrideLogs.isEmpty()) {
            logger.lifecycle(green("Overridden versions:"));
            overrideLogs.values().stream()
                    .sorted()
                    .forEach(logger::lifecycle);
        }

        if (!applyLogs.isEmpty()) {
            logger.lifecycle(green("\nApplied versions:"));
            applyLogs.values().stream()
                    .sorted()
                    .forEach(logger::lifecycle);
        }
    }

    private String green(String text) {
        return Painter.green(text);
    }

    private String cyan(String text) {
        return Painter.cyan(text);
    }

    private String yellow(String text, String dep) {
        return Painter.yellow(text, dep);
    }
}