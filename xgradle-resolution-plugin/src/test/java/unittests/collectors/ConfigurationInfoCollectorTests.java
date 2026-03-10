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
package unittests.collectors;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.altlinux.xgradle.impl.collectors.CollectorsModule;
import org.altlinux.xgradle.impl.utils.logging.LoggingModule;
import org.altlinux.xgradle.impl.model.ConfigurationInfoSnapshot;
import org.altlinux.xgradle.interfaces.collectors.ConfigurationInfoCollector;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unittests.TestGradleUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigurationInfoCollector contract")
class ConfigurationInfoCollectorTests {

    @Mock
    private Gradle gradle;

    @Test
    @DisplayName("Collects configuration names and test flags")
    void collectsConfigurationUsage() {
        Project root = TestGradleUtils.newJavaProject("root");
        root.getDependencies().add("implementation", "com.acme:lib:1.0");
        root.getDependencies().add("testImplementation", "com.acme:testlib:2.0");

        TestGradleUtils.gradleWithProjects(gradle, root);

        Injector injector = Guice.createInjector(new CollectorsModule(), new LoggingModule());
        ConfigurationInfoCollector collector = injector.getInstance(ConfigurationInfoCollector.class);

        ConfigurationInfoSnapshot snapshot = collector.collect(gradle);

        assertTrue(snapshot.getDependencyConfigNames().get("com.acme:lib").contains("implementation"));
        assertTrue(snapshot.getDependencyConfigNames().get("com.acme:testlib").contains("testImplementation"));

        assertEquals(Boolean.FALSE, snapshot.getTestDependencyFlags().get("com.acme:lib"));
        assertEquals(Boolean.TRUE, snapshot.getTestDependencyFlags().get("com.acme:testlib"));

        assertEquals(Set.of("implementation"), snapshot.getDependencyConfigNames().get("com.acme:lib"));
    }
}
