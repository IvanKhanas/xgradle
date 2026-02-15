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
package org.altlinux.xgradle.impl.parsers;

import org.apache.maven.model.Model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects and resolves Maven properties from a POM hierarchy.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class PomPropertiesCollector {

    Map<String, String> collect(List<Model> pomHierarchy) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.putIfAbsent("project.build.sourceEncoding", "UTF-8");
        properties.putIfAbsent("project.reporting.outputEncoding", "UTF-8");

        if (pomHierarchy == null) {
            return properties;
        }

        for (Model model : pomHierarchy) {
            if (model == null) {
                continue;
            }

            putIfNotEmpty(properties, "project.groupId", model.getGroupId());
            putIfNotEmpty(properties, "groupId", model.getGroupId());
            putIfNotEmpty(properties, "project.artifactId", model.getArtifactId());
            putIfNotEmpty(properties, "artifactId", model.getArtifactId());
            putIfNotEmpty(properties, "project.version", model.getVersion());
            putIfNotEmpty(properties, "version", model.getVersion());
            putIfNotEmpty(properties, "project.packaging", model.getPackaging());
            putIfNotEmpty(properties, "packaging", model.getPackaging());

            if (model.getProperties() != null) {
                model.getProperties().forEach(
                        (key, value) -> {
                            if (key != null && value != null) {
                                properties.put(
                                        key.toString(),
                                        value.toString()
                                );
                            }
                        }
                );
            }
        }
        return properties;
    }

    String resolve(String value, Map<String, String> properties) {
        if (value == null || properties == null) {
            return value;
        }

        String current = value;

        for (int iteration = 0; iteration < 20; iteration++) {
            StringBuilder buffer = new StringBuilder();
            int position = 0;
            boolean changed = false;

            while (position < current.length()) {
                int start = current.indexOf("${", position);
                if (start < 0) {
                    buffer.append(current.substring(position));
                    break;
                }

                int end = current.indexOf('}', start + 2);
                if (end < 0) {
                    buffer.append(current.substring(position));
                    break;
                }

                buffer.append(current, position, start);

                String key = current.substring(start + 2, end);
                String replacement = properties.get(key);

                if (replacement != null) {
                    buffer.append(replacement);
                    changed = true;
                } else {
                    buffer.append("${").append(key).append("}");
                }

                position = end + 1;
            }

            if (!changed) {
                break;
            }
            current = buffer.toString();
        }
        return current;
    }

    private static void putIfNotEmpty(
            Map<String, String> map,
            String key,
            String value
    ) {
        if (value != null && !value.trim().isEmpty()) {
            map.putIfAbsent(key, value);
        }
    }
}
