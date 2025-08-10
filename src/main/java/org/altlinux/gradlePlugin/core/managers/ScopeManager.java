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

    public void updateScope(String dependencyKey, String newScope) {
        if (newScope == null || newScope.isEmpty()) return;
        String currentScope = dependencyScopes.get(dependencyKey);
        if (currentScope == null || hasHigherPriority(newScope, currentScope)) {
            dependencyScopes.put(dependencyKey, newScope);
        }
    }

    public String getScope(String dependencyKey) {
        return dependencyScopes.getOrDefault(dependencyKey, "compile");
    }

    private boolean hasHigherPriority(String newScope, String currentScope) {
        int newPriority = getPriorityIndex(newScope);
        int currentPriority = getPriorityIndex(currentScope);
        return newPriority < currentPriority;
    }

    private int getPriorityIndex(String scope) {
        for (int i = 0; i < SCOPE_PRIORITY.length; i++) {
            if (SCOPE_PRIORITY[i].equals(scope)) return i;
        }
        return Integer.MAX_VALUE;
    }
}