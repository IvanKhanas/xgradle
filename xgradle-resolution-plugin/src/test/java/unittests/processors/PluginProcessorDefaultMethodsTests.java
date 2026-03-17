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
package unittests.processors;

import org.altlinux.xgradle.interfaces.processors.PluginProcessor;
import org.gradle.api.initialization.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("PluginProcessor defaults")
class PluginProcessorDefaultMethodsTests {

    @Test
    @DisplayName("configurePluginResolution delegates to process")
    void configurePluginResolutionDelegatesToProcess() {
        Settings settings = mock(Settings.class);
        NoopPluginProcessor processor = new NoopPluginProcessor();

        processor.configurePluginResolution(settings);

        assertTrue(processor.processCalled);
    }

    @Test
    @DisplayName("Default resolved plugin artifacts are empty")
    void defaultResolvedPluginArtifactsAreEmpty() {
        NoopPluginProcessor processor = new NoopPluginProcessor();

        assertAll(
                () -> assertTrue(processor.getResolvedPluginArtifacts().isEmpty()),
                () -> assertTrue(processor.getResolvedPluginArtifacts() != null)
        );
    }

    private static final class NoopPluginProcessor implements PluginProcessor {
        private boolean processCalled;

        @Override
        public void process(Settings settings) {
            processCalled = true;
        }
    }
}
