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
import com.google.inject.Singleton;

import org.altlinux.xgradle.interfaces.resolution.ResolutionReporter;
import org.altlinux.xgradle.interfaces.resolution.SystemDependencyResolution;
import org.gradle.api.invocation.Gradle;
/**
 * Resolution for System Dependency.
 * Implements {@link SystemDependencyResolution}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */

@Singleton
final class DefaultSystemDependencyResolution implements SystemDependencyResolution {

    private final DefaultResolutionPipeline pipeline;
    private final ResolutionReporter reporter;

    @Inject
    DefaultSystemDependencyResolution(DefaultResolutionPipeline pipeline, ResolutionReporter reporter) {
        this.pipeline = pipeline;
        this.reporter = reporter;
    }

    @Override
    public void run(Gradle gradle) {
        ResolutionContext ctx = new ResolutionContext(gradle);
        pipeline.run(ctx);
        reporter.report(ctx);
    }
}
