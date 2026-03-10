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
package org.altlinux.xgradle.impl.extensions;

import org.altlinux.xgradle.impl.utils.config.XGradleConfig;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
/**
 * Provides access to system-level dependency paths for the plugin.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
public class SystemDepsExtension {

    private static final String JAVA_LIBRARY_DIR_KEY = "java.library.dir";
    private static final String MAVEN_POMS_DIR_KEY = "maven.poms.dir";
    private static final String PATH_SEPARATOR = ",";
    private static final String CONFIG_FILE_HINT = "~/.xgradle/xgradle.config";
    private static final Logger LOGGER = Logging.getLogger(SystemDepsExtension.class);
    private static final AtomicBoolean MISSING_JARS_PATH_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean MISSING_POMS_PATH_LOGGED = new AtomicBoolean(false);

    public static String getJarsPath() {
        return getProperty(JAVA_LIBRARY_DIR_KEY, MISSING_JARS_PATH_LOGGED);
    }

    public static List<File> getJarsPaths() {
        return splitPathProperty(getJarsPath());
    }

    public static String getPomsPath() {
        return getProperty(MAVEN_POMS_DIR_KEY, MISSING_POMS_PATH_LOGGED);
    }

    private static List<File> splitPathProperty(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        StringTokenizer tokenizer = new StringTokenizer(value, PATH_SEPARATOR);
        return Collections.list(tokenizer).stream()
                .map(Object::toString)
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(File::new)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    private static String getProperty(String key, AtomicBoolean missingLogged) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String configValue = XGradleConfig.getConfigProperty(key);
        if (configValue != null && !configValue.isBlank()) {
            return configValue;
        }
        if (missingLogged.compareAndSet(false, true)) {
            LOGGER.warn(
                    "Missing required property: {}\n" +
                            "  Provide via JVM arg: -D{}=...\n" +
                            "  Or add to config: {}\n" +
                            "  Example: -D{}={}\n",
                    key, key, CONFIG_FILE_HINT, key, exampleValue(key)
            );
        }
        return null;
    }

    private static String exampleValue(String key) {
        if (JAVA_LIBRARY_DIR_KEY.equals(key)) {
            return "/usr/share/java,/usr/local/share/java";
        }
        if (MAVEN_POMS_DIR_KEY.equals(key)) {
            return "/usr/share/maven-poms";
        }
        return "/path/to/value";
    }
}
