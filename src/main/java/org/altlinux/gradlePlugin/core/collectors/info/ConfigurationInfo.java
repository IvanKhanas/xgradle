package org.altlinux.gradlePlugin.core.collectors.info;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;

public class ConfigurationInfo {
    private final String name;
    private final String type;
    private final boolean testConfigutation;

    public ConfigurationInfo(Configuration config) {
        this.name = config.getName();
        this.type = determineConfigurationType(config);
        this.testConfigutation = isTestConfigutation(config);
    }

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

    private boolean isTestConfigutation(Configuration config) {
        String name = config.getName().toLowerCase();
        return name.contains("test");
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isTestConfigutation() {
        return testConfigutation;
    }

    @Override
    public String toString() {
        return name + " [" + type + "]";
    }
}