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
package org.altlinux.xgradle.services;

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
 * <p>
 * This class traverses the parent chain of Maven POM files, loading each model in sequence
 * from the initial POM up to the root ancestor. It implements caching to avoid redundant
 * file reads and limits traversal depth to prevent infinite recursion.
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * PomHierarchyLoader loader = new PomHierarchyLoader();
 * List&lt;Model&gt; hierarchy = loader.loadHierarchy(projectPomPath, logger);
 * </pre>
 *
 * @author Ivan Khanas
 */
public class PomHierarchyLoader {
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final DefaultModelReader modelReader = new DefaultModelReader();

    /**
     * Loads the POM hierarchy starting from the specified file.
     * <p>
     * Traverses the parent chain of the initial POM file, loading each subsequent parent
     * until either the root POM is reached or the maximum depth (10 levels) is exceeded.
     * The returned list maintains child-to-ancestor order where the first element is the
     * initial POM and subsequent elements are its ancestors.
     * </p>
     *
     * @param pomPath path to the starting POM file (must exist and be readable)
     * @param logger Gradle logger for error reporting and debugging
     *
     * @return hierarchical list of POM models in child-to-ancestor order (never null)
     *
     * @throws SecurityException if file read permissions are insufficient
     */
    public List<Model> loadHierarchy(Path pomPath, Logger logger) {
        Deque<Model> stack = new ArrayDeque<>();
        Path currentPath = pomPath;
        int depth = 0;
        final int MAX_DEPTH = 10;

        while (currentPath != null && depth < MAX_DEPTH) {
                Model model = loadModel(currentPath, logger);
            if (model == null) break;

            stack.push(model);
            Parent parent = model.getParent();
            if (parent == null) break;

            currentPath = resolveParentPath(currentPath, parent);
            depth++;
        }
        return new ArrayList<>(stack);
    }

    /**
     * Loads a POM model from the filesystem with caching.
     * <p>
     * Subsequent calls for the same path will return the cached model instance.
     * </p>
     *
     * @param pomPath filesystem path to the POM file
     * @param logger logger for error reporting
     *
     * @return parsed POM model, or {@code null} if reading fails
     */
    private Model loadModel(Path pomPath, Logger logger) {
        return modelCache.computeIfAbsent(pomPath.toString(), k -> {
            try (InputStream is = Files.newInputStream(pomPath)) {
                return modelReader.read(is, null);
            } catch (Exception e) {
                logger.debug("Failed to load POM: " + pomPath, e);
                return null;
            }
        });
    }

    /**
     * Resolves the parent POM's path relative to a child POM.
     * <p>
     * Constructs the parent POM path by combining the child POM's directory with
     * the parent's artifact ID and ".pom" extension. This assumes parent POMs
     * are located in the same directory as their children.
     * </p>
     *
     * @param childPath filesystem path to the child POM file
     * @param parent parent POM metadata from the child's model
     *
     * @return resolved path to the parent POM file
     */
    private Path resolveParentPath(Path childPath, Parent parent) {
        return childPath.getParent().resolve(parent.getArtifactId() + ".pom");
    }
}