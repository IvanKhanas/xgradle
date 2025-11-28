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
package org.altlinux.xgradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.publish.PublicationContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Copy;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class XGradlePublishingConventions implements Plugin<Project> {
    private static final Logger logger = Logging.getLogger("PublishingLogger");

    @Override
    public void apply(@NotNull Project project) {
        XGradlePublishingConventionsExtension extension = project.getExtensions()
                .create("xgradlePublishingConventions", XGradlePublishingConventionsExtension.class);

        project.afterEvaluate(p -> configurePublishing(p, extension));
    }

    private void configurePublishing(Project project, XGradlePublishingConventionsExtension extension) {
        configureMavenPublications(project, extension);

        if (extension.getEnableCopyPublications().getOrElse(false)) {
            configureCopyPublicationsTask(project);
            configureTaskDependencies(project);
        }
    }

    private void configureMavenPublications(Project project, XGradlePublishingConventionsExtension extension) {
        project.getExtensions().configure(PublishingExtension.class, publishing -> {
            publishing.repositories(RepositoryHandler::mavenLocal);

            publishing.publications((PublicationContainer publications) -> {
                configureMainPublication(project, publications, extension);

                if (extension.getEnablePluginMarker().getOrElse(false)) {
                    configurePluginMarkerPublication(project, publications, extension);
                }
            });
        });
    }

    private void configureMainPublication(
            Project project,
            PublicationContainer publications,
            XGradlePublishingConventionsExtension extension
    ) {
        publications.create(project.getName(), MavenPublication.class, publication -> {
            publication.setArtifactId(project.getName());

            if (extension.getMainArtifactTaskName().isPresent()) {
                String mainArtifactTaskName = extension.getMainArtifactTaskName().get();
                if (project.getTasks().findByName(mainArtifactTaskName) != null) {
                    publication.artifact(project.getTasks().getByName(mainArtifactTaskName));
                }
            }

            if (extension.getJavadocJarTaskName().isPresent()) {
                String javadocJarTaskName = extension.getJavadocJarTaskName().get();
                if (project.getTasks().findByName(javadocJarTaskName) != null) {
                    publication.artifact(project.getTasks().getByName(javadocJarTaskName));
                }
            }

            if (extension.getSourcesJarTaskName().isPresent()) {
                String sourcesJarTaskName = extension.getSourcesJarTaskName().get();
                if (project.getTasks().findByName(sourcesJarTaskName) != null) {
                    publication.artifact(project.getTasks().getByName(sourcesJarTaskName));
                }
            }

            publication.pom(pom -> {
                if (extension.getProjectName().isPresent()) {
                    pom.getName().set(extension.getProjectName());
                }

                if (extension.getProjectUrl().isPresent()) {
                    pom.getUrl().set(extension.getProjectUrl());
                }

                if (extension.getProjectDescription().isPresent()) {
                    pom.getDescription().set(extension.getProjectDescription());
                }

                pom.licenses(licenses -> licenses.license(license -> {
                    if (extension.getLicenseName().isPresent()) {
                        license.getName().set(extension.getLicenseName());
                    }
                    if (extension.getLicenseUrl().isPresent()) {
                        license.getUrl().set(extension.getLicenseUrl());
                    }
                }));

                if (extension.getDevelopers().isPresent() && !extension.getDevelopers().get().isEmpty()) {
                    pom.developers(developers -> extension.getDevelopers().get()
                            .forEach(dev -> developers.developer(developer -> {
                                developer.getId().set(dev.getId());
                                developer.getName().set(dev.getName());
                                if (dev.getEmail() != null) {
                                    developer.getEmail().set(dev.getEmail());
                                }
                                if (dev.getUrl() != null) {
                                    developer.getUrl().set(dev.getUrl());
                                }
                            })));
                }
            });
        });
    }

    private void configurePluginMarkerPublication(
            Project project,
            PublicationContainer publications,
            XGradlePublishingConventionsExtension extension
    ) {
        publications.create("pluginMarkerMaven", MavenPublication.class, publication -> {
            publication.setGroupId(project.getGroup() + ".gradle.plugin");
            publication.setArtifactId(project.getGroup() + ".gradle.plugin");
            publication.setVersion(project.getVersion().toString());

            publication.pom(pom -> {
                pom.getName().set(project.getRootProject().getName() + " Plugin Marker");
                pom.getDescription().set("Plugin marker for " + project.getRootProject().getName());

                if (extension.getProjectUrl().isPresent()) {
                    pom.getUrl().set(extension.getProjectUrl());
                }

                pom.licenses(licenses -> licenses.license(license -> {
                    if (extension.getLicenseName().isPresent()) {
                        license.getName().set(extension.getLicenseName());
                    }
                    if (extension.getLicenseUrl().isPresent()) {
                        license.getUrl().set(extension.getLicenseUrl());
                    }
                }));
            });
        });
    }

    private void configureCopyPublicationsTask(Project project) {
        project.getTasks().register("copyPublicationsToDist", Copy.class, task -> {
            task.dependsOn("publishToMavenLocal");

            String groupPath = project.getGroup().toString().replace('.', '/');
            String artifactId = project.getName();
            String version = project.getVersion().toString();
            File mavenRepo = new File(System.getProperty("user.home"), ".m2/repository");
            File publicationDir = new File(mavenRepo, groupPath + "/" + artifactId + "/" + version);

            task.from(publicationDir, copySpec -> {
                copySpec.include(artifactId + "-" + version + ".jar");
                copySpec.include(artifactId + "-" + version + ".pom");
                copySpec.include(artifactId + "-" + version + "-javadoc.jar");
                copySpec.include(artifactId + "-" + version + "-sources.jar");

                copySpec.rename(filename -> {
                    if (filename.equals(artifactId + "-" + version + ".jar")) {
                        return artifactId + ".jar";
                    } else if (filename.equals(artifactId + "-" + version + ".pom")) {
                        return artifactId + ".pom";
                    } else if (filename.equals(artifactId + "-" + version + "-javadoc.jar")) {
                        return artifactId + "-javadoc.jar";
                    } else if (filename.equals(artifactId + "-" + version + "-sources.jar")) {
                        return artifactId + "-sources.jar";
                    }
                    return filename;
                });
            });

            task.into(project.getLayout().getBuildDirectory().dir("dist"));
            task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);

            logger.lifecycle("\nCopied all publications into -> {} for project: {}",
                    project.getLayout().getBuildDirectory().dir("dist").get().getAsFile(),
                    project.getName());
        });
    }

    private void configureTaskDependencies(Project project) {
        project.getTasks().named("publishToMavenLocal", task -> task.finalizedBy("copyPublicationsToDist"));

        if (project.getTasks().findByName("build") != null) {
            project.getTasks().named("build", task -> task.dependsOn("copyPublicationsToDist"));
        }
    }
}