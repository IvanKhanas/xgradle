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

public class RepositoryManager {
    private static final String JARS_PATH = SystemDepsExtension.getJarsPath();
    private Logger logger;

    public RepositoryManager(Logger logger) {
        this.logger = logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void configurePluginsRepository(Settings settings, File baseDir) {
        List<File> dirs = scanDirectories(baseDir);
        settings.getPluginManagement().getRepositories().flatDir(repo -> {
            repo.setName("SystemPluginsRepo");
            dirs.forEach(repo::dir);
            logger.lifecycle("Configured PluginManagement repository with {} directories", dirs.size());
        });
    }

    public void configureDependenciesRepository(RepositoryHandler repos) {
        File libDir = new File(JARS_PATH);
        validateDirectory(libDir);

        String repoName = "SystemDepsRepo-" + UUID.randomUUID();
        FlatDirectoryArtifactRepository flatRepo = createFlatRepository(repos, repoName, libDir);

        repos.remove(flatRepo);
        repos.addFirst(flatRepo);
    }

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

    private void validateDirectory(File dir) {
        if (!dir.canRead() || !dir.isDirectory()) {
            throw new GradleException("Invalid lib directory: " + dir.getAbsolutePath());
        }
    }

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