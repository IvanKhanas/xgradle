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
package org.altlinux.xgradle.impl.resolvers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.altlinux.xgradle.interfaces.resolvers.ArtifactResolver;
import org.altlinux.xgradle.interfaces.services.VersionScanner;
import org.altlinux.xgradle.impl.enums.MavenScope;
import org.altlinux.xgradle.impl.model.MavenCoordinate;
import org.gradle.api.logging.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
/**
 * Resolver for Artifact.
 * Implements {@link ArtifactResolver}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
public final class DefaultArtifactResolver implements ArtifactResolver {

    private final VersionScanner versionScanner;

    private Map<String, MavenCoordinate> systemArtifacts = Collections.emptyMap();
    private Set<String> notFound = Collections.emptySet();

    @Inject
    public DefaultArtifactResolver(VersionScanner versionScanner) {
        this.versionScanner = versionScanner;
    }

    @Override
    public void resolve(Set<String> dependencies, Logger logger) {
        systemArtifacts = versionScanner.scanSystemArtifacts(dependencies);
        notFound = versionScanner.getNotFoundDependencies();
    }

    @Override
    public void filter() {
        systemArtifacts.entrySet().removeIf(e ->
                MavenScope.TEST.equals(e.getValue().getScope()) || e.getValue().isBom()
        );
    }

    @Override
    public Map<String, MavenCoordinate> getSystemArtifacts() {
        return systemArtifacts;
    }

    @Override
    public Set<String> getNotFoundDependencies() {
        return notFound;
    }
}
