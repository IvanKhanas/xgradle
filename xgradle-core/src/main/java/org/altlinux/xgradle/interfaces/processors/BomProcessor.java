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
package org.altlinux.xgradle.interfaces.processors;

import org.gradle.api.invocation.Gradle;

import java.util.Set;

/**
 * Processor for BOM (Bill of Materials) dependencies.
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public interface BomProcessor {


    final class Context {
        private final Gradle gradle;
        private final Set<String> projectDependencies;

        public Context(Gradle gradle, Set<String> projectDependencies) {
            this.gradle = gradle;
            this.projectDependencies = projectDependencies;
        }

        public Gradle getGradle() { return gradle; }
        public Set<String> getProjectDependencies() { return projectDependencies; }
    }

    /**
     * Main entrypoint for BOM processing.
     */
    BomResult process(Context context);

    /**
     * Removes BOM dependencies from Gradle configurations so that
     * only managed dependencies remain attached.
     *
     * @param gradle current Gradle instance
     * @param processedBoms BOM identifiers ("groupId:artifactId") to remove
     */
    void removeBomsFromConfigurations(Gradle gradle, Set<String> processedBoms);
}
