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
package unittests.context;

import org.altlinux.xgradle.interfaces.context.RepositoryContext;
import org.gradle.api.initialization.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author Ivan Khanas xeno@altlinux.org
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RepositoryContext contract")
class RepositoryContextTests {

    @Mock
    private Settings settings;

    @Test
    @DisplayName("Stores settings and baseDir")
    void storesValues() {
        File baseDir = new File("base");

        RepositoryContext ctx = new RepositoryContext(settings, baseDir);
        assertSame(settings, ctx.getSettings());
        assertSame(baseDir, ctx.getBaseDir());
    }

    @Test
    @DisplayName("Rejects null settings or baseDir")
    void rejectsNulls() {
        File baseDir = new File("base");

        assertThrows(NullPointerException.class, () -> new RepositoryContext(null, baseDir));
        assertThrows(NullPointerException.class, () -> new RepositoryContext(settings, null));
    }
}
