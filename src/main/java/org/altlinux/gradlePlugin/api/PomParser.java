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
package org.altlinux.gradlePlugin.api;

import org.altlinux.gradlePlugin.model.MavenCoordinate;

import org.gradle.api.logging.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

/**
 * Interface for pom parsing classes.
 *
 * @author Ivan Khanas
 */
public interface PomParser {
    /**
     * Method for determining the appropriate pom file for a dependency.
     *
     * @param pomPath path to Pom file to be parsed
     * @param logger logger for diagnostic messages
     *
     * @return valid MavenCoordinate
     */
    MavenCoordinate parsePom(Path pomPath, Logger logger);

    /**
     * Method for parsing dependencyManagement block of a pom file to collect
     * a valid Maven coordinates of each dependency.
     *
     * @param pomPath path to the Pom file whose dependencyManagement block will be parsed
     * @param logger logger for diagnostic messages
     *
     * @return ArrayList of valid MavenCoordinates
     */
    ArrayList<MavenCoordinate> parseDependencyManagement(Path pomPath, Logger logger);

    /**
     * Method for parsing dependencies block of a pom file to collect
     * a valid Maven coordinates of each dependency.
     *
     * @param pomPath path to the Pom file whose dependencies block will be parsed
     * @param logger logger for diagnostic messages
     *
     * @return ArrayList of valid MavenCoordinates
     */
    ArrayList<MavenCoordinate> parseDependencies(Path pomPath, Logger logger);

    /**
     * Method for parsing properties of a Pom file.
     *
     * @param pomPath path to the Pom file whose properties block will be parsed
     * @param logger logger for diagnostic messages
     *
     * @return Map of dependencies names and versions
     */
    Map<String, String> parseProperties(Path pomPath, Logger logger);
}