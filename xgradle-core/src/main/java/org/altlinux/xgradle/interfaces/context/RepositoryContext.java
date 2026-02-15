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

package org.altlinux.xgradle.interfaces.context;

import org.gradle.api.initialization.Settings;

import java.io.File;
import java.util.Objects;


public final class RepositoryContext {

    private final Settings settings;
    private final File baseDir;

    public RepositoryContext(Settings settings, File baseDir) {
        this.settings = Objects.requireNonNull(settings);
        this.baseDir = Objects.requireNonNull(baseDir);
    }

    public Settings getSettings() {
        return settings;
    }

    public File getBaseDir() {
        return baseDir;
    }
}
