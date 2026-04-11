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
package unittests.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.altlinux.xgradle.impl.di.XGradleToolModule;
import org.altlinux.xgradle.interfaces.application.Application;
import org.altlinux.xgradle.interfaces.cli.CommandExecutor;
import org.altlinux.xgradle.interfaces.cli.CommandLineParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("XGradleToolModule contract")
class XGradleToolModuleTests {

    @Test
    @DisplayName("Creates injector and resolves top-level bindings")
    void createsInjectorAndResolvesTopLevelBindings() {
        Injector injector = Guice.createInjector(new XGradleToolModule());

        assertAll(
                () -> assertNotNull(injector.getInstance(Application.class)),
                () -> assertNotNull(injector.getInstance(CommandExecutor.class)),
                () -> assertNotNull(injector.getInstance(CommandLineParser.class))
        );
    }
}
