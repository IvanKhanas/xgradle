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
/**
  * Defines X gradle publishing conventions operations.

 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

public interface XGradlePublishingConventionsExtension {
/**
  * Returns project URL.

 */
    Property<String> getProjectUrl();
/**
  * Returns project description.

 */
    Property<String> getProjectDescription();
/**
  * Returns enable plugin marker.

 */
    Property<Boolean> getEnablePluginMarker();
/**
  * Returns enable copy publications.

 */
    Property<Boolean> getEnableCopyPublications();
/**
  * Returns project name.

 */

    Property<String> getProjectName();
/**
  * Returns license name.

 */
    Property<String> getLicenseName();
/**
  * Returns license URL.

 */
    Property<String> getLicenseUrl();
/**
  * Returns developers.

 */
    ListProperty<Developer> getDevelopers();
/**
  * Returns main artifact task name.

 */

    Property<String> getMainArtifactTaskName();
/**
  * Returns javadoc JAR task name.

 */
    Property<String> getJavadocJarTaskName();
/**
  * Returns sources JAR task name.

 */
    Property<String> getSourcesJarTaskName();
/**
  * With shadow JAR.

 */

    default void withShadowJar() {
        getMainArtifactTaskName().set("shadowJar");
    }
/**
  * With JAR.

 */

    default void withJar() {
        getMainArtifactTaskName().set("jar");
    }
/**
  * With javadoc JAR.

 */

    default void withJavadocJar() {
        getJavadocJarTaskName().set("javadocJar");
    }
/**
  * With sources JAR.

 */

    default void withSourcesJar() {
        getSourcesJarTaskName().set("sourcesJar");
    }
/**
  * Developer the operation.

 */

    default void developer(String id, String name, String email) {
        getDevelopers().add(new Developer(id, name, email));
    }
/**
  * Developer the operation.

 */

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