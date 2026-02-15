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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PomXmlBuilder {

    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;

    private Parent parent;

    private boolean includeDependenciesBlock;
    private boolean includeDependencyManagementBlock;

    private final List<Dep> dependencies = new ArrayList<>();
    private final List<Dep> managedDependencies = new ArrayList<>();
    private final Map<String, String> properties = new LinkedHashMap<>();

    public static PomXmlBuilder pom() {
        return new PomXmlBuilder();
    }

    public PomXmlBuilder groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public PomXmlBuilder artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public PomXmlBuilder version(String version) {
        this.version = version;
        return this;
    }

    public PomXmlBuilder packaging(String packaging) {
        this.packaging = packaging;
        return this;
    }

    public PomXmlBuilder property(String key, String value) {
        this.properties.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return this;
    }

    public PomXmlBuilder properties(Map<String, String> props) {
        if (props != null) {
            props.forEach(this::property);
        }
        return this;
    }

    public PomXmlBuilder parent(String groupId, String artifactId, String version) {
        this.parent = new Parent(groupId, artifactId, version);
        return this;
    }

    public PomXmlBuilder dependenciesBlock() {
        this.includeDependenciesBlock = true;
        return this;
    }

    public PomXmlBuilder dependencyManagementBlock() {
        this.includeDependencyManagementBlock = true;
        return this;
    }

    public PomXmlBuilder dep(String groupId, String artifactId, String version, String scope) {
        this.dependencies.add(new Dep(groupId, artifactId, version, scope));
        this.includeDependenciesBlock = true;
        return this;
    }

    public PomXmlBuilder managedDep(String groupId, String artifactId, String version, String scope) {
        this.managedDependencies.add(new Dep(groupId, artifactId, version, scope));
        this.includeDependencyManagementBlock = true;
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("  <modelVersion>4.0.0</modelVersion>\n");

        if (parent != null) {
            sb.append("  <parent>\n");
            if (parent.groupId != null) sb.append("    <groupId>").append(parent.groupId).append("</groupId>\n");
            if (parent.artifactId != null) sb.append("    <artifactId>").append(parent.artifactId).append("</artifactId>\n");
            if (parent.version != null) sb.append("    <version>").append(parent.version).append("</version>\n");
            sb.append("  </parent>\n");
        }

        if (groupId != null) sb.append("  <groupId>").append(groupId).append("</groupId>\n");
        if (artifactId != null) sb.append("  <artifactId>").append(artifactId).append("</artifactId>\n");
        if (version != null) sb.append("  <version>").append(version).append("</version>\n");
        if (packaging != null) sb.append("  <packaging>").append(packaging).append("</packaging>\n");

        if (!properties.isEmpty()) {
            sb.append("  <properties>\n");
            for (Map.Entry<String, String> e : properties.entrySet()) {
                sb.append("    <").append(e.getKey()).append(">")
                        .append(e.getValue())
                        .append("</").append(e.getKey()).append(">\n");
            }
            sb.append("  </properties>\n");
        }

        if (includeDependenciesBlock) {
            sb.append("  <dependencies>\n");
            for (Dep d : dependencies) {
                sb.append(depXml(d, 4)).append("\n");
            }
            sb.append("  </dependencies>\n");
        }

        if (includeDependencyManagementBlock) {
            sb.append("  <dependencyManagement>\n");
            sb.append("    <dependencies>\n");
            for (Dep d : managedDependencies) {
                sb.append(depXml(d, 6)).append("\n");
            }
            sb.append("    </dependencies>\n");
            sb.append("  </dependencyManagement>\n");
        }

        sb.append("</project>\n");
        return sb.toString();
    }

    public Path writeTo(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, build().getBytes(StandardCharsets.UTF_8));
            return path;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeBytes(Path path, byte[] data) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Model readModel(Path pomPath) {
        try (Reader reader = Files.newBufferedReader(pomPath, StandardCharsets.UTF_8)) {
            return new MavenXpp3Reader().read(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String depXml(Dep d, int indent) {
        String i = spaces(indent);
        String i2 = spaces(indent + 2);

        StringBuilder sb = new StringBuilder();
        sb.append(i).append("<dependency>\n");
        sb.append(i2).append("<groupId>").append(d.groupId).append("</groupId>\n");
        sb.append(i2).append("<artifactId>").append(d.artifactId).append("</artifactId>\n");
        if (d.version != null) sb.append(i2).append("<version>").append(d.version).append("</version>\n");
        if (d.scope != null) sb.append(i2).append("<scope>").append(d.scope).append("</scope>\n");
        sb.append(i).append("</dependency>");
        return sb.toString();
    }

    private static String spaces(int n) {
        return " ".repeat(Math.max(0, n));
    }

    private static final class Parent {
        final String groupId;
        final String artifactId;
        final String version;

        Parent(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }
    }

    private static final class Dep {
        final String groupId;
        final String artifactId;
        final String version;
        final String scope;

        Dep(String groupId, String artifactId, String version, String scope) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.scope = scope;
        }
    }
}
