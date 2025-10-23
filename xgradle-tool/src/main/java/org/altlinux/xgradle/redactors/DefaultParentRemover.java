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
package org.altlinux.xgradle.redactors;

import com.google.inject.Singleton;

import org.altlinux.xgradle.api.redactors.ParentRedactor;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

/**
 * Default implementation of ParentRedactor for removing parent blocks from POM files.
 * Uses Maven model API to read and modify POM files.
 *
 * @author Ivan Khanas
 */
@Singleton
public class DefaultParentRemover implements ParentRedactor {
    private static final Logger logger = LoggerFactory.getLogger("XGradleLogger");

    /**
     * Removes the parent block from the specified POM file.
     *
     * @param pomPath path to the POM file to modify
     * @throws RuntimeException if an error occurs during POM file processing
     */
    @Override
    public void removeParent(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;

        File pomFile = pomPath.toFile();

        try {
            model = reader.read(new FileReader(pomFile));

            if(model.getParent() != null) {
                model.setParent(null);
            }else {
                logger.warn("POM file hasn`t parent block, cannot remove: " + pomPath);
                return;
            }

            FileWriter fileWriter = new FileWriter(pomFile);

            MavenXpp3Writer mavenXpp3Writer = new MavenXpp3Writer();
            mavenXpp3Writer.write(fileWriter, model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
