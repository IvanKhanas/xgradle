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
package unittests;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.testfixtures.ProjectBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
public final class TestGradleUtils {

    private TestGradleUtils() {
    }

    public static Project newJavaProject(String name) {
        Project project = ProjectBuilder.builder().withName(name).build();
        project.getPlugins().apply("java");
        return project;
    }

    public static Project newJavaSubproject(String name, Project parent) {
        Project project = ProjectBuilder.builder().withName(name).withParent(parent).build();
        project.getPlugins().apply("java");
        return project;
    }

    public static Gradle gradleWithProjects(Gradle gradle, Project root, Project... others) {
        lenient().when(gradle.getRootProject()).thenReturn(root);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Action<Project> action = invocation.getArgument(0);
            action.execute(root);
            if (others != null) {
                for (Project project : others) {
                    action.execute(project);
                }
            }
            return null;
        }).when(gradle).allprojects(any(Action.class));
        return gradle;
    }
}
