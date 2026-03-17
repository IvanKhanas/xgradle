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
package unittests.cli;

import com.beust.jcommander.JCommander;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.altlinux.xgradle.impl.cli.CliModule;
import org.altlinux.xgradle.impl.cli.CustomXgradleFormatter;
import org.altlinux.xgradle.interfaces.cli.CommandExecutor;
import org.altlinux.xgradle.interfaces.cli.CommandLineParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@DisplayName("CliModule contract")
class CliModuleTests {

    @Test
    @DisplayName("Binds parser executor logger and JCommander")
    void bindsCoreCliServices() {
        Injector injector = Guice.createInjector(new CliModule());

        CommandExecutor executor = injector.getInstance(CommandExecutor.class);
        CommandLineParser parser = injector.getInstance(CommandLineParser.class);
        Logger logger1 = injector.getInstance(Logger.class);
        Logger logger2 = injector.getInstance(Logger.class);
        JCommander commander1 = injector.getInstance(JCommander.class);
        JCommander commander2 = injector.getInstance(JCommander.class);

        assertAll(
                () -> assertEquals(
                        "org.altlinux.xgradle.impl.cli.DefaultCommandExecutor",
                        executor.getClass().getName()
                ),
                () -> assertEquals(
                        "org.altlinux.xgradle.impl.cli.DefaultCommandLineParser",
                        parser.getClass().getName()
                ),
                () -> assertEquals("XGradleLogger", logger1.getName()),
                () -> assertSame(logger1, logger2),
                () -> assertEquals("xgradle-cli", commander1.getProgramName()),
                () -> assertTrue(commander1.getUsageFormatter() instanceof CustomXgradleFormatter),
                () -> assertSame(commander1, commander2)
        );
    }
}
