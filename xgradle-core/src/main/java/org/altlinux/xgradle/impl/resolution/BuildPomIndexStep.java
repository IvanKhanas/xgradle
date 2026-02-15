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

import org.altlinux.xgradle.interfaces.indexing.PomIndexBuilder;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;

/**
 * Builds a POM index from collected POM files.
 * Implements {@link ResolutionStep}.
 *
 * @author Ivan Khanas <xeno@altlinux.org>
 */
@Singleton
final class BuildPomIndexStep implements ResolutionStep {

    private final PomIndexBuilder builder;

    @Inject
    BuildPomIndexStep(PomIndexBuilder builder) {
        this.builder = builder;
    }

    @Override
    public String name() {
        return "build-pom-index";
    }

    @Override
    public void execute(ResolutionContext context) {
        context.setPomIndex(builder.build(context.getPomFiles()));
    }
}
