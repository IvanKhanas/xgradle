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
package org.altlinux.xgradle.impl.maven;

import com.google.inject.Inject;
import org.altlinux.xgradle.interfaces.maven.PomHierarchyLoader;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.DefaultModelReader;
import org.gradle.api.logging.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads a hierarchy of Maven POM models starting from a specified POM file.
 * Implements {@link PomHierarchyLoader}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class MavenPomHierarchyLoader implements PomHierarchyLoader {
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final DefaultModelReader modelReader = new DefaultModelReader();

    private final Logger logger;

    @Inject
    MavenPomHierarchyLoader(Logger logger) {
        this.logger = logger;
    }

    @Override
    public List<Model> loadHierarchy(Path pomPath) {
        Deque<Model> stack = new ArrayDeque<>();
        Path currentPath = pomPath;
        int depth = 0;
        final int MAX_DEPTH = 10;

        while (currentPath != null && depth < MAX_DEPTH) {
                Model model = loadModel(currentPath);
            if (model == null) break;

            stack.push(model);
            Parent parent = model.getParent();
            if (parent == null) break;

            currentPath = resolveParentPath(currentPath, parent);
            depth++;
        }
        return new ArrayList<>(stack);
    }

    private Model loadModel(Path pomPath) {
        return modelCache.computeIfAbsent(pomPath.toString(), k -> {
            try (InputStream is = Files.newInputStream(pomPath)) {
                return modelReader.read(is, null);
            } catch (Exception e) {
                logger.debug("Failed to load POM: " + pomPath, e);
                return null;
            }
        });
    }

    private Path resolveParentPath(Path childPath, Parent parent) {
        return childPath.getParent().resolve(parent.getArtifactId() + ".pom");
    }
}
