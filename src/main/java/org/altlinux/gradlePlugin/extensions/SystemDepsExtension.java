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
package org.altlinux.gradlePlugin.extensions;

/**
 * Provides access to system-level dependency paths for the plugin.
 *
 * <p>This class serves as a centralized configuration point for
 * locating system-provided Java libraries and Maven POM files.
 *
 * <p>Path resolution:
 * <ul>
 *   <li>Uses system properties for custom configuration</li>
 *   <li>Provides read-only access to resolved paths</li>
 * </ul>
 *
 * <p>System properties:
 * <ul>
 *   <li>{@code java.library.dir} - Set JAR directory</li>
 *   <li>{@code maven.poms.dir} - Set POM directory</li>
 * </ul>
 *
 * @author Ivan Khanas
 */
public class SystemDepsExtension {

    /**
     * Retrieves the configured path for system-provided JAR libraries.
     *
     * @return absolute path to the JAR directory
     */
    public static String getJarsPath() {
        return System.getProperty("java.library.dir");
    }

    /**
     * Retrieves the configured path for system-provided Maven POM files.
     *
     * @return absolute path to the POM directory
     */
    public static String getPomsPath() {
        return System.getProperty("maven.poms.dir");
    }
}
