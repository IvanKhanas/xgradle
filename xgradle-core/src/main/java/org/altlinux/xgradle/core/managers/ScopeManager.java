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
package org.altlinux.xgradle.core.managers;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages dependency scopes with priority-based updates.
 * <p>
 * This class maintains a mapping of dependency keys to their respective
 * scopes, allowing updates based on a predefined priority order. It ensures
 * that higher-priority scopes take precedence when updating.
 *
 * <p>Scope priorities (highest to lowest):
 * <ul>
 *   <li>compile</li>
 *   <li>runtime</li>
 *   <li>provided</li>
 *   <li>test</li>
 * </ul>
 *
 * @author Ivan Khanas
 */
public class ScopeManager {
    private final Map<String, String> dependencyScopes = new HashMap<>();
    private static final String[] SCOPE_PRIORITY = { "compile", "runtime", "provided", "test" };

    /**
     * Updates the scope of a dependency if the new scope has higher priority.
     *
     * <p>If the new scope {@code newScope} has a higher priority than the current scope
     * for the given dependency key {@code dependencyKey}, the scope will be updated.
     * Otherwise, the update will be ignored.
     *
     * @param dependencyKey the key identifying the dependency (e.g., group:name:version)
     * @param newScope the new scope to set (e.g., "compile", "runtime", "provided", "test")
     */
    public void updateScope(String dependencyKey, String newScope) {
        if (newScope == null || newScope.isEmpty()) return;
        String currentScope = dependencyScopes.get(dependencyKey);
        if (currentScope == null || hasHigherPriority(newScope, currentScope)) {
            dependencyScopes.put(dependencyKey, newScope);
        }
    }

    /**
     * Retrieves the scope associated with a given dependency key.
     *
     * <p>If no scope has been set for the dependency, returns "compile" as the default scope.
     *
     * @param dependencyKey the key identifying the dependency
     *
     * @return the current scope of the dependency
     */
    public String getScope(String dependencyKey) {
        return dependencyScopes.getOrDefault(dependencyKey, "compile");
    }

    /**
     * Determines if the new scope has a higher priority than the current scope.
     *
     * @param newScope the new scope to compare
     * @param currentScope the current scope to compare against
     *
     * @return {@code true} if {@code newScope} has higher priority, {@code false} otherwise
     */
    private boolean hasHigherPriority(String newScope, String currentScope) {
        int newPriority = getPriorityIndex(newScope);
        int currentPriority = getPriorityIndex(currentScope);
        return newPriority < currentPriority;
    }

    /**
     * Returns the priority index of a given scope.
     *
     * <p>Lower index values indicate higher priority.
     * If the scope is not recognized, returns {@code Integer.MAX_VALUE}.
     *
     * @param scope the scope to look up
     *
     * @return the priority index of the scope
     */
    private int getPriorityIndex(String scope) {
        for (int i = 0; i < SCOPE_PRIORITY.length; i++) {
            if (SCOPE_PRIORITY[i].equals(scope)) return i;
        }
        return Integer.MAX_VALUE;
    }
}