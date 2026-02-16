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
package org.altlinux.xgradle.impl.redactors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.redactors.PomRedactor;
import org.altlinux.xgradle.impl.model.DependencySpec;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * Redactor for POM.
 * Implements {@link PomRedactor}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class DefaultPomRedactor implements PomRedactor {

    private final Logger logger;

    @Inject
    DefaultPomRedactor(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void addDependency(Path pomPath, String coords) {
        DependencySpec spec = DependencySpec.parse(coords);
        Model model = readModel(pomPath);

        boolean changedDeps = addToList(ensureDependencies(model), spec);
        boolean changedMgmt = false;

        if (model.getDependencyManagement() != null) {
            changedMgmt = addToList(ensureDependencyManagementDeps(model.getDependencyManagement()), spec);
        }

        if (changedDeps || changedMgmt) {
            writeModel(pomPath, model);
        }
    }

    @Override
    public void removeDependency(Path pomPath, String coords) {
        DependencySpec spec = DependencySpec.parse(coords);
        Model model = readModel(pomPath);

        boolean removedDeps = removeFromList(ensureDependencies(model), spec);
        boolean removedMgmt = false;

        if (model.getDependencyManagement() != null) {
            removedMgmt = removeFromList(ensureDependencyManagementDeps(model.getDependencyManagement()), spec);
            if (model.getDependencyManagement().getDependencies().isEmpty()) {
                model.setDependencyManagement(null);
            }
        }

        if (removedDeps || removedMgmt) {
            writeModel(pomPath, model);
        }
    }

    @Override
    public void changeDependency(Path pomPath, String origCoords, String targetCoords) {
        DependencySpec from = DependencySpec.parse(origCoords);
        DependencySpec to = DependencySpec.parse(targetCoords);

        Model model = readModel(pomPath);

        boolean changedDeps = changeInList(ensureDependencies(model), from, to);
        boolean changedMgmt = false;

        if (model.getDependencyManagement() != null) {
            changedMgmt = changeInList(ensureDependencyManagementDeps(model.getDependencyManagement()), from, to);
        }

        if (changedDeps || changedMgmt) {
            writeModel(pomPath, model);
        }
    }

    @Override
    public void removeParent(Path pomPath) {
        Model model = readModel(pomPath);

        if (model.getParent() == null) {
            logger.warn("POM file hasn`t parent block, cannot remove: " + pomPath);
            return;
        }

        model.setParent(null);
        writeModel(pomPath, model);
    }

    private Model readModel(Path pomPath) {
        try (Reader reader = Files.newBufferedReader(pomPath, StandardCharsets.UTF_8)) {
            return new MavenXpp3Reader().read(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read POM: " + pomPath, e);
        }
    }

    private void writeModel(Path pomPath, Model model) {
        try (Writer writer = Files.newBufferedWriter(
                pomPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            new MavenXpp3Writer().write(writer, model);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write POM: " + pomPath, e);
        }
    }

    private static List<Dependency> ensureDependencies(Model model) {
        List<Dependency> deps = model.getDependencies();
        if (deps == null) {
            deps = new ArrayList<>();
            model.setDependencies(deps);
        }
        return deps;
    }

    private static List<Dependency> ensureDependencyManagementDeps(DependencyManagement dm) {
        List<Dependency> deps = dm.getDependencies();
        if (deps == null) {
            deps = new ArrayList<>();
            dm.setDependencies(deps);
        }
        return deps;
    }

    private boolean addToList(List<Dependency> list, DependencySpec spec) {
        boolean exists = list.stream().anyMatch(d -> sameGA(d, spec));
        if (exists) {
            return false;
        }
        list.add(toDependency(spec));
        return true;
    }

    private boolean removeFromList(List<Dependency> list, DependencySpec spec) {
        List<Dependency> filtered = list.stream()
                .filter(d -> !matches(d, spec))
                .collect(Collectors.toList());

        if (filtered.size() == list.size()) {
            return false;
        }
        list.clear();
        list.addAll(filtered);
        return true;
    }

    private boolean changeInList(List<Dependency> list, DependencySpec from, DependencySpec to) {
        List<Dependency> targets = list.stream()
                .filter(d -> matches(d, from))
                .collect(Collectors.toList());

        if (targets.isEmpty()) {
            return false;
        }

        targets.forEach(d -> apply(d, to));
        return true;
    }

    private static Dependency toDependency(DependencySpec spec) {
        Dependency d = new Dependency();
        d.setGroupId(spec.getGroupId());
        d.setArtifactId(spec.getArtifactId());
        spec.getVersion().ifPresent(d::setVersion);
        spec.getScope().ifPresent(d::setScope);
        return d;
    }

    private static void apply(Dependency d, DependencySpec to) {
        d.setGroupId(to.getGroupId());
        d.setArtifactId(to.getArtifactId());
        d.setVersion(to.getVersion().orElse(null));
        d.setScope(to.getScope().orElse(null));
    }

    private static boolean sameGA(Dependency d, DependencySpec spec) {
        return Objects.equals(d.getGroupId(), spec.getGroupId())
                && Objects.equals(d.getArtifactId(), spec.getArtifactId());
    }

    private static boolean matches(Dependency d, DependencySpec spec) {
        if (!sameGA(d, spec)) return false;

        Optional<String> v = spec.getVersion();
        Optional<String> s = spec.getScope();

        boolean versionOk = v.map(val -> Objects.equals(d.getVersion(), val)).orElse(true);
        boolean scopeOk = s.map(val -> Objects.equals(d.getScope(), val)).orElse(true);

        return versionOk && scopeOk;
    }
}
