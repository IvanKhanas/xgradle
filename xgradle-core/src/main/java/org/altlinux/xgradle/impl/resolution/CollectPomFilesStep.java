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
package org.altlinux.xgradle.impl.resolution;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.altlinux.xgradle.impl.utils.config.XGradleConfig;
import org.altlinux.xgradle.interfaces.collectors.PomFilesCollector;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;

import java.nio.file.Path;
import java.util.List;

/**
 * Collects POM files from the configured POM root directory.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class CollectPomFilesStep implements ResolutionStep {

    private final Provider<PomFilesCollector> collectorProvider;

    @Inject
    CollectPomFilesStep(Provider<PomFilesCollector> collectorProvider) {
        this.collectorProvider = collectorProvider;
    }

    @Override
    public String name() {
        return "collect-pom-files";
    }

    @Override
    public void execute(ResolutionContext context) {
        PomFilesCollector collector = collectorProvider.get();

        String pomsDir = XGradleConfig.getProperty("maven.poms.dir", "");
        Path rootDir = Path.of(pomsDir);
        List<Path> collected = collector.collect(rootDir);

        context.getPomFiles().clear();
        context.getPomFiles().addAll(collected);
    }
}
