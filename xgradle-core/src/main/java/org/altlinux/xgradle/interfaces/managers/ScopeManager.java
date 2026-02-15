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

package org.altlinux.xgradle.interfaces.managers;

import org.altlinux.xgradle.impl.enums.MavenScope;

import java.util.Map;
/**
  * Manages scope.

 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public interface ScopeManager {

    /**
     * Updates the stored scope for a dependency in the provided map if the new scope has higher priority.
     *
     * @param scopes storage for dependency scopes
     * @param dependencyKey dependency identifier (for example "groupId:artifactId")
     * @param newScope new scope candidate
     */
    void updateScope(Map<String, MavenScope> scopes, String dependencyKey, MavenScope newScope);
/**
  * Returns scope.

 */

    MavenScope getScope(Map<String, MavenScope> scopes, String dependencyKey);
}
