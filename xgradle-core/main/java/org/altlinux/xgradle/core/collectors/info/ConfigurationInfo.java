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
package org.altlinux.xgradle.core.collectors.info;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;

/**
 * Provides metadata about a Gradle {@link Configuration}, including its
 * name, derived type, and whether it is associated with test code.
 *
 * <p>This class is used by other components of the plugin (e.g.
 * artifact configurators) to make decisions about how dependencies
 * should be assigned to Gradle configurations such as {@code api},
 * {@code implementation}, {@code runtimeOnly}, or {@code testImplementation}.</p>
 *
 * <p>The classification logic considers both Gradle attributes
 * (such as {@code org.gradle.usage}) and the configuration name.</p>
 *
 * @author Ivan Khanas
 */
public class ConfigurationInfo {
    private final String name;
    private final String type;
    private final boolean testConfigutation;

    /**
     * Creates a {@code ConfigurationInfo} instance from a Gradle configuration.
     *
     * <p>During construction the following metadata is derived:</p>
     * <ul>
     *   <li>{@link #name} → the configuration name (e.g. {@code implementation})</li>
     *   <li>{@link #type} → a normalized type label such as {@code API}, {@code IMPLEMENTATION},
     *       {@code RUNTIME}, {@code TEST}, or {@code UNKNOWN}, determined by
     *       {@link #determineConfigurationType(Configuration)}</li>
     *   <li>{@link #testConfigutation} → whether the configuration is considered a test
     *       configuration, determined by {@link #isTestConfigutation(Configuration)}</li>
     * </ul>
     *
     * @param config the Gradle {@link Configuration} to inspect
     */
    public ConfigurationInfo(Configuration config) {
        this.name = config.getName();
        this.type = determineConfigurationType(config);
        this.testConfigutation = isTestConfigutation(config);
    }

    /**
     * Determines the configuration type from Gradle metadata.
     *
     * <p>The logic first checks the {@code org.gradle.usage} attribute
     * (if available):</p>
     * <ul>
     *   <li>{@code java-api} → {@code API}</li>
     *   <li>{@code java-runtime} → {@code RUNTIME}</li>
     * </ul>
     *
     * <p>If no matching attribute is present, the configuration name is analyzed:</p>
     * <ul>
     *   <li>contains {@code api} → {@code API}</li>
     *   <li>contains {@code implementation} → {@code IMPLEMENTATION}</li>
     *   <li>contains {@code runtime} → {@code RUNTIME}</li>
     *   <li>contains {@code test} → {@code TEST}</li>
     * </ul>
     *
     * <p>If none of the above match, the type is reported as {@code UNKNOWN}.</p>
     *
     * @param config the configuration to inspect
     *
     * @return a normalized configuration type string
     */
    private String determineConfigurationType(Configuration config) {
        Attribute<String> usageAttributes = Attribute.of("org.gradle.usage", String.class);
        if (config.getAttributes().contains(usageAttributes)) {
            String usage = config.getAttributes().getAttribute(usageAttributes);
            if ("java-api".equals(usage)) return  "API";
            if ("java-runtime".equals(usage)) return  "RUNTIME";
        }

        String name = config.getName().toLowerCase();
        if (name.contains("api")) return  "API";
        if (name.contains("implementation")) return  "IMPLEMENTATION";
        if (name.contains("runtime")) return  "RUNTIME";
        if (name.contains("test")) return  "TEST";
        return  "UNKNOWN";
    }

    /**
     * Determines whether the given configuration is test-related.
     *
     * <p>A configuration is considered test-related if its name
     * contains the substring {@code test} (case-insensitive).
     * This heuristic aligns with standard Gradle configurations
     * such as {@code testImplementation} and {@code testRuntimeOnly}.</p>
     *
     * @param config the configuration to inspect
     *
     * @return {@code true} if the configuration is test-related, {@code false} otherwise
     */
    private boolean isTestConfigutation(Configuration config) {
        String name = config.getName().toLowerCase();
        return name.contains("test");
    }

    /**
     * Returns the original configuration name.
     *
     * @return the configuration name (e.g. {@code implementation}, {@code api})
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the derived configuration type.
     *
     * <p>Possible values are: {@code API}, {@code IMPLEMENTATION},
     * {@code RUNTIME}, {@code TEST}, or {@code UNKNOWN}.</p>
     *
     * @return the normalized type of this configuration
     */
    public String getType() {
        return type;
    }

    /**
     * Indicates whether the configuration is test-related.
     *
     * @return {@code true} if the configuration is associated with tests,
     *         {@code false} otherwise
     */
    public boolean isTestConfigutation() {
        return testConfigutation;
    }

    /**
     * Returns a string representation of this configuration info.
     *
     * <p>The format is {@code <name> [<type>]}, for example:
     * {@code implementation [IMPLEMENTATION]} or {@code testRuntimeOnly [TEST]}.</p>
     *
     * @return a human-readable string summarizing the configuration
     */
    @Override
    public String toString() {
        return name + " [" + type + "]";
    }
}