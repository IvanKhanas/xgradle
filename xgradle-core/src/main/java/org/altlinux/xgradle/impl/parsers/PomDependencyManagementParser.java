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

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses dependencyManagement sections from a Maven POM hierarchy.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
final class PomDependencyManagementParser {

    Map<String, MavenCoordinate> parse(
            List<Model> pomHierarchy,
            Map<String, String> properties,
            PomPropertiesCollector propertiesCollector
    ) {
        Map<String, MavenCoordinate> managedByGroupAndArtifact =
                new LinkedHashMap<>();

        if (pomHierarchy == null) {
            return managedByGroupAndArtifact;
        }

        for (Model model : pomHierarchy) {
            if (model == null
                    || model.getDependencyManagement() == null
                    || model.getDependencyManagement().getDependencies() == null) {
                continue;
            }

            for (Dependency dependency
                    : model.getDependencyManagement().getDependencies()) {

                MavenCoordinate managedDependency =
                        PomDependencyUtils.convertDependency(dependency);

                managedDependency =
                        PomDependencyUtils.resolveProperties(
                                managedDependency,
                                properties,
                                propertiesCollector
                        );

                if (!managedDependency.isValid()) {
                    continue;
                }

                String groupAndArtifact =
                        managedDependency.getGroupId()
                                + ":" + managedDependency.getArtifactId();

                managedByGroupAndArtifact.put(
                        groupAndArtifact,
                        managedDependency
                );
            }
        }
        return managedByGroupAndArtifact;
    }
}
