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
package org.altlinux.xgradle.impl.containers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.collectors.PomCollector;
import org.altlinux.xgradle.interfaces.containers.PomContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of PomContainer for managing POM file collections.
 * Implements {@link PomContainer}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class DefaultPomContainer implements PomContainer {
    private final PomCollector pomCollector;

    @Inject
    DefaultPomContainer(PomCollector pomCollector) {
        this.pomCollector = pomCollector;
    }

    @Override
    public Set<Path> getAllPoms(String searchingDir) {
        return pomCollector.collectAll(searchingDir);
    }

    @Override
    public Set<Path> getSelectedPoms(String searchingDir, List<String> artifactName) {
        return pomCollector.collectSelected(searchingDir, artifactName);
    }
}
