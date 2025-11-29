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
package org.altlinux.xgradle.impl.utils.logging;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.utils.ui.Painter;
import org.gradle.api.logging.Logger;

import java.util.Map;
import java.util.Set;
import static org.altlinux.xgradle.impl.utils.ui.Painter.*;

/**
 * Provides structured logging for dependency resolution processes in Gradle builds.
 *
 * <p>Formats and outputs information about different stages of dependency management:
 * <ul>
 *   <li>Initial dependency discovery</li>
 *   <li>Artifact resolution results</li>
 *   <li>Dependency categorization</li>
 *   <li>Configuration assignments</li>
 *   <li>Version substitution operations</li>
 * </ul>
 *
 * <p>All output uses color-coded formatting when ANSI coloring is enabled.
 *
 * @author Ivan Khanas
 */
public class DependencyLogger {

    /**
     * Logs a titled section header with visual separation.
     *
     * @param title Section title text
     * @param logger Gradle logger instance
     */
    public void logSection(String title, Logger logger) {
        logger.lifecycle(green("\n--- " + title + " ---"));
    }

    /**
     * Logs the count of initially discovered dependencies.
     *
     * @param dependencies Set of dependency identifiers (format: "groupId:artifactId")
     * @param logger Gradle logger instance
     */
    public void logInitialDependencies(Set<String> dependencies, Logger logger) {
        logger.lifecycle("Found {} dependencies", dependencies.size());
    }

    /**
     * Logs resolved artifacts with their Maven coordinates.
     *
     * @param artifacts Map of resolved artifacts (key: dependency identifier)
     * @param logger Gradle logger instance
     */
    public void logResolvedArtifacts(Map<String, MavenCoordinate> artifacts, Logger logger) {
        logger.lifecycle("Resolved {} artifacts", artifacts.size());
        logger.info("System artifacts:");
        artifacts.forEach((k, v) ->
                logger.info(" - {}:{}:{}", v.getGroupId(), v.getArtifactId(), v.getVersion())
        );
    }

    /**
     * Logs new dependencies discovered during transitive resolution.
     *
     * @param newDeps Set of new dependency identifiers
     * @param logger Gradle logger instance
     */
    public void logNewDependencies(Set<String> newDeps, Logger logger) {
        logger.lifecycle("Found {} dependencies", newDeps.size());
        newDeps.forEach(dep -> logger.info(" - {}", dep));
    }

    /**
     * Logs dependencies categorized as test context.
     *
     * @param testDeps Set of test-scoped dependency identifiers
     * @param logger Gradle logger instance
     */
    public void logTestContextDependencies(Set<String> testDeps, Logger logger) {
        logger.lifecycle("Test context dependencies: {}", testDeps.size());
        testDeps.forEach(dep -> logger.info(" - {}", dep));
    }

    /**
     * Logs artifacts assigned to Gradle configurations.
     *
     * @param artifacts Map of configuration assignments (key: configuration name)
     * @param logger Gradle logger instance
     */
    public void logConfigurationArtifacts(Map<String, Set<String>> artifacts, Logger logger) {
        artifacts.forEach((cfg, arts) -> {
            logger.lifecycle("Configuration '{}' ({} artifacts):", cyan(cfg), arts.size());
            arts.forEach(a -> logger.lifecycle(" - {}", a));
        });
    }

    /**
     * Logs unresolved dependencies with warning formatting.
     *
     * @param notFound Dependencies not found in repositories
     * @param skipped Transitive dependencies skipped during processing
     * @param logger Gradle logger instance
     */
    public void logSkippedDependencies(Set<String> notFound, Set<String> skipped, Logger logger) {
        if (!notFound.isEmpty()) {
            logger.lifecycle(yellow("Not found BOM dependencies:"));
            notFound.forEach(d -> logger.lifecycle(" - {}", d));
        }

        if (!skipped.isEmpty()) {
            logger.lifecycle(yellow("Not found transitive dependencies:"));
            skipped.forEach(d -> logger.lifecycle(" - {}", d));
        }
    }

    /**
     * Logs version substitution operations.
     *
     * @param overrideLogs Version override operations
     * @param applyLogs BOM-applied version operations
     * @param logger Gradle logger instance
     */
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
            logger.info(green("\nApplied versions:"));
            applyLogs.values().stream()
                    .sorted()
                    .forEach(logger::info);
        }
    }

    /**
     * Applies green color formatting to text if ANSI coloring is enabled.
     *
     * @param text Input text to format
     * @return Formatted text string
     *
     * @see Painter#green(String)
     */
    private String green(String text) {
        return Painter.green(text);
    }

    /**
     * Applies cyan color formatting to text if ANSI coloring is enabled.
     *
     * @param text Input text to format
     * @return Formatted text string
     *
     * @see Painter#cyan(String)
     */
    private String cyan(String text) {
        return Painter.cyan(text);
    }
}