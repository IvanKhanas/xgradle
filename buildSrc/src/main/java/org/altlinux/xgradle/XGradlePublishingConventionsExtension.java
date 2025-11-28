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

import org.gradle.api.provider.Property;
import org.gradle.api.provider.ListProperty;

public interface XGradlePublishingConventionsExtension {
    Property<String> getProjectUrl();
    Property<String> getProjectDescription();
    Property<Boolean> getEnablePluginMarker();
    Property<Boolean> getEnableCopyPublications();

    Property<String> getProjectName();
    Property<String> getLicenseName();
    Property<String> getLicenseUrl();
    ListProperty<Developer> getDevelopers();

    Property<String> getMainArtifactTaskName();
    Property<String> getJavadocJarTaskName();
    Property<String> getSourcesJarTaskName();

    default void withShadowJar() {
        getMainArtifactTaskName().set("shadowJar");
    }

    default void withJar() {
        getMainArtifactTaskName().set("jar");
    }

    default void withJavadocJar() {
        getJavadocJarTaskName().set("javadocJar");
    }

    default void withSourcesJar() {
        getSourcesJarTaskName().set("sourcesJar");
    }

    default void developer(String id, String name, String email) {
        getDevelopers().add(new Developer(id, name, email));
    }

    default void developer(String id, String name, String email, String url) {
        getDevelopers().add(new Developer(id, name, email, url));
    }
}

class Developer {
    private String id;
    private String name;
    private String email;
    private String url;

    public Developer(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Developer(String id, String name, String email, String url) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.url = url;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}