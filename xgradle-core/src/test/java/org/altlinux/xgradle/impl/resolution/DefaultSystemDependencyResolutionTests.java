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

import org.altlinux.xgradle.interfaces.resolution.ResolutionReporter;
import org.altlinux.xgradle.interfaces.resolution.ResolutionStep;
import org.gradle.api.invocation.Gradle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultSystemDependencyResolution")
class DefaultSystemDependencyResolutionTests {

    @Mock
    private ResolutionStep step;

    @Mock
    private ResolutionReporter reporter;

    @Mock
    private Gradle gradle;

    @Test
    @DisplayName("Runs pipeline and reports with same context")
    void runsPipelineAndReporter() {
        DefaultResolutionPipeline pipeline = new DefaultResolutionPipeline(List.of(step));

        DefaultSystemDependencyResolution resolution =
                new DefaultSystemDependencyResolution(pipeline, reporter);

        resolution.run(gradle);

        ArgumentCaptor<ResolutionContext> captor = ArgumentCaptor.forClass(ResolutionContext.class);
        verify(reporter).report(captor.capture());
        ResolutionContext ctx = captor.getValue();

        assertSame(gradle, ctx.getGradle());
    }
}
