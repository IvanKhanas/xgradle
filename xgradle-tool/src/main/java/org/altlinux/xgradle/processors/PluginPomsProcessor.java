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
package org.altlinux.xgradle.processors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.altlinux.xgradle.api.processors.PomProcessor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class PluginPomsProcessor implements PomProcessor<HashMap<String, Path>> {
    private final PomParser<HashMap<String, Path>> pluginsParser;

    @Inject
    public PluginPomsProcessor(@Named("gradlePlugins") PomParser<HashMap<String, Path>> pluginsParser) {
        this.pluginsParser = pluginsParser;
    }
    @Override
    public PluginPomsProcessor process() {
        return this;
    }

    @Override
    public HashMap<String, Path> pomsFromDirectory(String searchingDir, Optional<String> artifactName) {
        return pluginsParser.parsePoms().getArtifactCoords(searchingDir, artifactName);
    }

}
