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
package org.altlinux.xgradle.impl.model;

import org.altlinux.xgradle.impl.enums.ConfigurationType;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;

import java.util.Locale;
/**
 * Implementation for Configuration Info.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public final class ConfigurationInfo {

    private static final Attribute<String> USAGE_ATTRIBUTE =
            Attribute.of("org.gradle.usage", String.class);

    private final String name;
    private final ConfigurationType type;
    private final boolean testConfiguration;

    public ConfigurationInfo(Configuration config) {
        this.name = config.getName();
        this.type = determineConfigurationType(config);
        this.testConfiguration = isTestConfiguration(config);
    }

    private ConfigurationType determineConfigurationType(Configuration config) {
        String n = config.getName().toLowerCase(Locale.ROOT);
        switch (n) {
            case "api":
                return ConfigurationType.API;
            case "implementation":
                return ConfigurationType.IMPLEMENTATION;
            case "compileonly":
                return ConfigurationType.COMPILE_ONLY;
            case "compileonlyapi":
                return ConfigurationType.COMPILE_ONLY_API;
            case "runtimeonly":
            case "runtime":
                return ConfigurationType.RUNTIME;
            case "annotationprocessor":
                return ConfigurationType.ANNOTATION_PROCESSOR;
            case "testimplementation":
                return ConfigurationType.TEST;
            case "testcompileonly":
                return ConfigurationType.TEST_COMPILE_ONLY;
            case "testruntimeonly":
                return ConfigurationType.TEST_RUNTIME_ONLY;
            case "testannotationprocessor":
                return ConfigurationType.TEST_ANNOTATION_PROCESSOR;
            default:
                break;
        }

        if (config.getAttributes().contains(USAGE_ATTRIBUTE)) {
            String usage = config.getAttributes().getAttribute(USAGE_ATTRIBUTE);
            if ("java-api".equals(usage)) return ConfigurationType.API;
            if ("java-runtime".equals(usage)) return ConfigurationType.RUNTIME;
        }
        return ConfigurationType.UNKNOWN;
    }

    private boolean isTestConfiguration(Configuration config) {
        return config.getName().toLowerCase(Locale.ROOT).contains("test");
    }

    public String getName() {
        return name;
    }

    public ConfigurationType getType() {
        return type;
    }

    @Override
    public String toString() {
        return name + " [" + type + "]";
    }

    public boolean hasTestConfiguration() {
        return testConfiguration;
    }
}
