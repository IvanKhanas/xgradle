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
package org.altlinux.xgradle.collectors;

import org.altlinux.xgradle.api.collectors.PomCollector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultPomCollector implements PomCollector {

    @Override
    public HashMap<String, Path> collectAll(String searchingDir) {
        HashMap<String, Path> result = new HashMap<>();
        try (Stream<Path> paths = Files.walk(Path.of(searchingDir), Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pom"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                        String artifactName = baseName.replaceAll("-\\d+(\\.\\d+)*$", "");
                        result.put(artifactName, path);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public HashMap<String, Path> collectSelected(String searchingDir, String artifactName) {
        HashMap<String, Path> result = new HashMap<>();
        try (Stream<Path> paths = Files.walk(Path.of(searchingDir), Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pom"))
                    .filter(path -> path.getFileName().toString().startsWith(artifactName))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                        String artifactId = baseName.replaceAll("-\\d+(\\.\\d+)*$", "");
                        result.put(artifactId, path);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}