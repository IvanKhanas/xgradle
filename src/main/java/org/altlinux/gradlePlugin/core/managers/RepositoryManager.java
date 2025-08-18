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
package org.altlinux.gradlePlugin.core.managers;

import org.altlinux.gradlePlugin.extensions.SystemDepsExtension;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages the addition of system-level dependency repositories to a Gradle build.
 *
 * <p>This class is responsible for configuring a Flat Directory repository
 * pointing to system-provided Java library directories, allowing Gradle
 * builds to resolve dependencies from these local paths.
 *
 * <p>The main repository path is retrieved from the {@link SystemDepsExtension},
 * which resolves the directory based on system properties.
 *
 * <p>The repository is added with the highest priority (at the beginning of the repository list).
 * It also recursively scans subdirectories up to a depth of 3 to include them as repository directories.
 *
 * <p>If the directory is invalid or unreadable, a {@link GradleException} is thrown.
 *
 * @author Ivan Khanas
 */
public class RepositoryManager {
    private static final String JARS_PATH = SystemDepsExtension.getJarsPath();
    private Logger logger;

    /**
     * Constructs a RepositoryManager with the specified logger.
     *
     * @param logger logger to output lifecycle and error messages
     */
    public RepositoryManager(Logger logger) {
        this.logger = logger;
    }


    /**
     * Sets the logger instance used by this manager.
     *
     * @param logger a Gradle logger
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Configures a Flat Directory repository for Gradle plugin resolution.
     *
     * <p>This method is intended for use in {@link Settings} phase, where
     * plugin repositories are declared. It scans the given base directory
     * (including its subdirectories up to depth 3) and adds them as search
     * locations for resolving plugins.
     *
     * <p>The repository is registered under the name {@code SystemPluginsRepo}.
     *
     * @param settings the Gradle settings instance used to configure plugin management
     * @param baseDir  the base directory containing plugin JARs and subdirectories
     */
    public void configurePluginsRepository(Settings settings, File baseDir) {
        List<File> dirs = scanDirectories(baseDir);
        settings.getPluginManagement().getRepositories().flatDir(repo -> {
            repo.setName("SystemPluginsRepo");
            dirs.forEach(repo::dir);
            logger.lifecycle("Configured PluginManagement repository with {} directories", dirs.size());
        });
    }

    /**
     * Configures a Flat Directory repository for system-provided dependencies.
     *
     * <p>The base library path is taken from {@link SystemDepsExtension#getJarsPath()}.
     * The repository is created with a unique name ({@code SystemDepsRepo-<UUID>}),
     * scanned for subdirectories, and inserted at the beginning of the repository
     * list to ensure it has the highest priority.
     *
     * <p>If the base directory is invalid or unreadable, a {@link GradleException}
     * will be thrown.
     *
     * @param repos the Gradle repository handler used to register repositories
     */
    public void configureDependenciesRepository(RepositoryHandler repos) {
        File libDir = new File(JARS_PATH);
        validateDirectory(libDir);

        String repoName = "SystemDepsRepo-" + UUID.randomUUID();
        FlatDirectoryArtifactRepository flatRepo = createFlatRepository(repos, repoName, libDir);

        repos.remove(flatRepo);
        repos.addFirst(flatRepo);
    }

    /**
     * Scans the given base directory for all subdirectories up to a maximum depth of 3.
     *
     * <p>Includes the base directory itself.
     *
     * @param baseDir the directory to scan
     *
     * @return a list of directories including the base directory and its subdirectories
     */
    public List<File> scanDirectories(File baseDir) {
        List<File> allDirs = new ArrayList<>(List.of(baseDir));
        try {
            Files.walk(baseDir.toPath(), 3)
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(baseDir.toPath()))
                    .forEach(path -> allDirs.add(path.toFile()));
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Directory scan error: {}", e.getMessage());
            }
        }
        return allDirs;
    }

    /**
     * Validates that the specified directory exists, is a directory, and is readable.
     *
     * @param dir the directory to validate
     * @throws GradleException if the directory is invalid or unreadable
     */
    private void validateDirectory(File dir) {
        if (!dir.canRead() || !dir.isDirectory()) {
            throw new GradleException("Invalid lib directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * Creates a Flat Directory repository configuration for the given directory.
     *
     * <p>Includes the base directory and all its subdirectories up to a depth of 3.
     *
     * @param repos the repository handler (unused here but required for DSL)
     * @param repoName the name to assign to the repository
     * @param libDir the base directory for repository artifacts
     *
     * @return the configured {@link FlatDirectoryArtifactRepository} instance
     */
    private FlatDirectoryArtifactRepository createFlatRepository(
            RepositoryHandler repos,
            String repoName,
            File libDir
    ) {
        return repos.flatDir(repo -> {
            repo.setName(repoName);
            List<File> allDirs = scanDirectories(libDir);
            allDirs.forEach(repo::dir);
            if (logger != null) {
                logger.lifecycle("Configured DependencyManagement repository with {} directories", allDirs.size());
            }
        });
    }
}