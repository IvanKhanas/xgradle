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

import org.altlinux.xgradle.interfaces.collectors.PomCollector;

import com.google.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Default implementation of PomCollector for collecting POM files from directories.
 * Implements {@link PomCollector}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPomCollector implements PomCollector {

    @Override
    public Set<Path> collectAll(String searchingDir) {
        Set<Path> result = new HashSet<>();
        try (Stream<Path> paths = Files.walk(Path.of(searchingDir), Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pom"))
                    .forEach(result::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Set<Path> collectSelected(String searchingDir, List<String> artifactNames) {

        Objects.requireNonNull(artifactNames, "artifactNames can not be null");

        Set<Path> result = new HashSet<>();
        try (Stream<Path> paths = Files.walk(Path.of(searchingDir), Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pom"))
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return artifactNames.stream()
                                .anyMatch(fileName::startsWith);
                    })
                    .forEach(result::add);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
