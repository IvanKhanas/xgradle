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
package org.altlinux.xgradle.impl.managers;

import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.managers.ScopeManager;
import org.altlinux.xgradle.impl.enums.MavenScope;

import java.util.EnumMap;

import java.util.Map;

/**
 * Default implementation of ScopeManager that uses MavenScope enum and priority-based updates.
 * Implements {@link ScopeManager}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class MavenScopeManager implements ScopeManager {
    private static final EnumMap<MavenScope, Integer> PRIORITY = new EnumMap<>(MavenScope.class);

    static {
        PRIORITY.put(MavenScope.COMPILE, 0);
        PRIORITY.put(MavenScope.RUNTIME, 1);
        PRIORITY.put(MavenScope.PROVIDED, 2);
        PRIORITY.put(MavenScope.TEST, 3);
    }

    @Override
    public void updateScope(Map<String, MavenScope> scopes, String dependencyKey, MavenScope newScope) {
        if (scopes == null || newScope == null) {
            return;
        }

        MavenScope currentScope = scopes.get(dependencyKey);
        if (currentScope == null || hasHigherPriority(newScope, currentScope)) {
            scopes.put(dependencyKey, newScope);
        }
    }

    @Override
    public MavenScope getScope(Map<String, MavenScope> scopes, String dependencyKey) {
        if (scopes == null) {
            return MavenScope.COMPILE;
        }
        return (scopes.getOrDefault(dependencyKey, MavenScope.COMPILE));
    }

    private boolean hasHigherPriority(MavenScope newScope, MavenScope currentScope) {
        Integer newPriority = PRIORITY.get(newScope);
        Integer currentPriority = PRIORITY.get(currentScope);


        if (newPriority == null) {
            return false;
        }
        if (currentPriority == null) {
            return true;
        }
        return newPriority < currentPriority;
    }
}
