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

package org.altlinux.xgradle.impl.collectors;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.collectors.PomFilesCollector;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
/**
 * Collector for POM Files.
 * Implements {@link PomFilesCollector}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class DefaultPomFilesCollector implements PomFilesCollector {

    private static final int MAX_SEARCH_DEPTH = 10;

    private final Logger logger;

    @Inject
    DefaultPomFilesCollector(Logger logger) {
        this.logger = logger;
    }

    @Override
    public List<Path> collect(Path rootDirectory) {
        List<Path> pomPaths = new ArrayList<>();

        if (rootDirectory == null || !Files.isDirectory(rootDirectory)) {
            logger.lifecycle("POM root directory is not valid: {}", rootDirectory);
            return pomPaths;
        }

        try (Stream<Path> stream = Files.walk(rootDirectory, MAX_SEARCH_DEPTH)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String s = p.toString();
                        return s.endsWith(".pom");
                    })
                    .forEach(pomPaths::add);
        } catch (IOException e) {
            logger.lifecycle("Error while scanning POM directory {}: {}", rootDirectory, e.getMessage());
        }

        return pomPaths;
    }
}
