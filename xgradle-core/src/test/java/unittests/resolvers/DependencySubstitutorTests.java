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
package unittests.resolvers;

import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.altlinux.xgradle.impl.resolvers.DefaultDependencySubstitutor;
import org.altlinux.xgradle.interfaces.resolvers.DependencySubstitutor;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencySubstitution;
import org.gradle.api.artifacts.DependencySubstitutions;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DependencySubstitutor contract")
class DependencySubstitutorTests {

    @Mock
    private Gradle gradle;

    @Mock
    private Project project;

    @Mock
    private ConfigurationContainer configs;

    @Mock
    private Configuration configuration;

    @Mock
    private ResolutionStrategy strategy;

    @Mock
    private DependencySubstitutions subs;

    @Mock
    private DependencySubstitution details;

    @Mock
    private ModuleComponentSelector selector;

    @Mock
    private ModuleComponentSelector targetSelector;

    @Test
    @DisplayName("Applies override substitution for system artifact version")
    void appliesOverrideSubstitution() {
        DependencySubstitutor substitutor = new DefaultDependencySubstitutor();

        when(project.getConfigurations()).thenReturn(configs);
        when(configuration.getResolutionStrategy()).thenReturn(strategy);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Action<Project> action = invocation.getArgument(0);
            action.execute(project);
            return null;
        }).when(gradle).allprojects(any(Action.class));

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Action<Configuration> action = invocation.getArgument(0);
            action.execute(configuration);
            return null;
        }).when(configs).all(any(Action.class));

        when(subs.module(any(String.class))).thenReturn(targetSelector);

        when(selector.getGroup()).thenReturn("g");
        when(selector.getModule()).thenReturn("a");
        when(selector.getVersion()).thenReturn("1.0");
        when(details.getRequested()).thenReturn(selector);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Action<DependencySubstitutions> action = invocation.getArgument(0);
            action.execute(subs);
            return null;
        }).when(strategy).dependencySubstitution(any(Action.class));

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Action<DependencySubstitution> action = invocation.getArgument(0);
            action.execute(details);
            return null;
        }).when(subs).all(any(Action.class));

        Map<String, Set<String>> requested = Map.of("g:a", Set.of("1.0"));
        Map<String, MavenCoordinate> systemArtifacts = Map.of(
                "g:a",
                MavenCoordinate.builder().groupId("g").artifactId("a").version("2.0").build()
        );

        substitutor.configure(
                gradle,
                requested,
                systemArtifacts,
                Map.of(),
                new java.util.HashMap<>(),
                new java.util.HashMap<>()
        );

        verify(subs).module("g:a:2.0");
        verify(details).useTarget(eq(targetSelector), anyString());
    }
}
