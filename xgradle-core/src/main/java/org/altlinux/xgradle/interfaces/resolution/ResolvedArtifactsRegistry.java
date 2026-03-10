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
package org.altlinux.xgradle.interfaces.resolution;

import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for resolved artifact files, exposed via Gradle extra properties.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public final class ResolvedArtifactsRegistry {

    public static final String EXTENSION_NAME = "xgradle.resolvedJars";

    private ResolvedArtifactsRegistry() {
    }

    @SuppressWarnings("unchecked")
    public static Set<File> getOrCreate(Gradle gradle) {
        return getOrCreate(gradle.getRootProject());
    }

    @SuppressWarnings("unchecked")
    public static Set<File> getOrCreate(Project project) {
        ExtraPropertiesExtension extra = project.getExtensions().getExtraProperties();
        Object value = extra.has(EXTENSION_NAME) ? extra.get(EXTENSION_NAME) : null;
        if (value instanceof Set) {
            return (Set<File>) value;
        }
        Set<File> created = Collections.newSetFromMap(new ConcurrentHashMap<>());
        extra.set(EXTENSION_NAME, created);
        return created;
    }

    @SuppressWarnings("unchecked")
    public static Set<File> get(Project project) {
        ExtraPropertiesExtension extra = project.getExtensions().getExtraProperties();
        Object value = extra.has(EXTENSION_NAME) ? extra.get(EXTENSION_NAME) : null;
        return value instanceof Set ? (Set<File>) value : null;
    }
}
