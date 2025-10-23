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
package integrationtests.parsertests;

import org.altlinux.xgradle.model.MavenCoordinate;
import org.altlinux.xgradle.services.DefaultPomParser;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * Integration tests for verifying the parsing of dependency management sections in Maven POM files.
 * <p>
 * This test class focuses on validating the functionality of the {@link DefaultPomParser#parseDependencyManagement(Path, Logger)}
 * method. It ensures that the parser correctly extracts and processes dependency information from the
 * {@code <dependencyManagement>} section of POM files, including:
 * <ul>
 *   <li>Identification of all managed dependencies</li>
 *   <li>Correct groupId, artifactId, and version resolution</li>
 *   <li>Proper handling of BOM (Bill of Materials) files</li>
 *   <li>Accurate version assignment for managed dependencies</li>
 * </ul>
 *
 * <p>Test scenarios include:
 * <ol>
 *   <li>Parsing simple BOM files</li>
 *   <li>Parsing complex BOM files with multiple dependencies</li>
 *   <li>Verifying the count of extracted dependencies</li>
 *   <li>Validating coordinate accuracy (groupId, artifactId)</li>
 *   <li>Checking version resolution correctness</li>
 * </ol>
 *
 * <p>Test resources are located in {@code xgradle-core/src/test/resources/poms/} and include:
 * <ul>
 *   <li>Sample BOM files from real-world projects</li>
 *   <li>POM files with varying complexity and structure</li>
 * </ul>
 *
 * @see DefaultPomParser
 * @see DefaultPomParser#parseDependencyManagement(Path, Logger)
 *
 * @author Ivan Khanas
 */
public class TestDependencyManagementBlockParser {
    private DefaultPomParser pomParser;
    private ArrayList<MavenCoordinate> parsedDeps;

    Logger logger;

    /**
     * Initializes test environment before each test method execution.
     * <p>
     * Creates a fresh instance of {@link DefaultPomParser} and configures a logger
     * specifically for the test class. This ensures test isolation and consistent
     * initial conditions for all test cases.
     */
    @BeforeEach
    public void prepareParser() {
        pomParser = new DefaultPomParser();
        logger = Logging.getLogger(this.getClass());
    }

    /**
     * Tests parsing of the JUnit Platform BOM file.
     * <p>
     * Verifies that the parser correctly handles a typical Bill of Materials (BOM)
     * file from the JUnit project. The test checks:
     * <ul>
     *   <li>All expected dependencies are present (JUnit Jupiter, Platform, Vintage)</li>
     *   <li>Dependencies are ordered as in the original POM</li>
     *   <li>GroupId and artifactId match expected values</li>
     *   <li>Version numbers are resolved correctly</li>
     *   <li>Total dependency count matches expectations</li>
     * </ul>
     *
     * @param tempDir temporary directory provided by JUnit for test file operations
     */
    @Test
    public void parseJunitBom(@TempDir Path tempDir) {
        preparePom("src/test/resources/poms/junit5/junit-bom.pom", tempDir);

        parsedDeps = pomParser.parseDependencyManagement(tempDir.resolve(Path.of("junit-bom.pom")), logger);

        assertFalse(parsedDeps.isEmpty());

        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.jupiter", "junit-jupiter", 0));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.jupiter", "junit-jupiter-api", 1));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.jupiter", "junit-jupiter-engine", 2));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.jupiter","junit-jupiter-migrationsupport" ,3));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.jupiter", "junit-jupiter-params",4));

        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform","junit-platform-commons",5));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform","junit-platform-console",6));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform","junit-platform-engine",7));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform","junit-platform-jfr",8));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform","junit-platform-launcher",9));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform","junit-platform-reporting",10));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform", "junit-platform-runner", 11));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform", "junit-platform-suite", 12));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform", "junit-platform-suite-api", 13));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform", "junit-platform-suite-commons", 14));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform", "junit-platform-suite-engine", 15));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.platform", "junit-platform-testkit", 16));

        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.junit.vintage", "junit-vintage-engine", 17));

        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-api", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-engine", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-migrationsupport", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-params", "5.8.2"));

        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-commons", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-console", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-engine", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-jfr", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-launcher", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-reporting", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-runner", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-suite", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-suite-api", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-suite-commons", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-suite-engine", "1.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-platform-testkit", "1.8.2"));

        assertTrue(checkDependencyVersion(parsedDeps, "junit-vintage-engine", "5.8.2"));

        assertEquals(18, parsedDeps.size());
    }

    /**
     * Tests parsing of the Maven Surefire BOM file.
     * <p>
     * Validates parser behavior with a more complex BOM file containing diverse
     * dependencies. The test verifies:
     * <ul>
     *   <li>Handling of dependencies from various sources (Apache Commons, Maven, etc.)</li>
     *   <li>Correct resolution of version numbers for 31 distinct dependencies</li>
     *   <li>Proper identification of test-scoped dependencies</li>
     *   <li>Accurate processing of plugin-related artifacts</li>
     * </ul>
     *
     * @param tempDir temporary directory provided by JUnit for test file operations
     */
    @Test
    public void parseSurefireBom(@TempDir Path tempDir) {
        preparePom("src/test/resources/poms/maven-surefire/surefire.pom", tempDir);

        parsedDeps = pomParser.parseDependencyManagement(tempDir.resolve(Path.of("surefire.pom")), logger);

        assertFalse(parsedDeps.isEmpty());
        assertEquals(31, parsedDeps.size());

        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.commons", "commons-compress", 0));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.commons", "commons-lang3", 1));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "commons-io", "commons-io", 2));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven.reporting", "maven-reporting-api", 3));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven", "maven-core", 4));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven", "maven-plugin-api", 5));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven", "maven-artifact", 6));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven", "maven-model", 7));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven", "maven-compat", 8));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven", "maven-settings", 9));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven.shared", "maven-shared-utils", 10));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven.reporting", "maven-reporting-impl", 11));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven.shared", "maven-common-artifact-filters", 12));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven.plugin-testing", "maven-plugin-testing-harness", 13));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.xmlunit", "xmlunit-core", 14));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "net.sourceforge.htmlunit", "htmlunit", 15));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.fusesource.jansi", "jansi", 16));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.apache.maven.shared", "maven-verifier", 17));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.codehaus.plexus", "plexus-java", 18));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.mockito", "mockito-core", 19));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.powermock", "powermock-core", 20));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.powermock", "powermock-module-junit4", 21));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.powermock", "powermock-api-mockito2", 22));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.powermock", "powermock-reflect", 23));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.javassist", "javassist", 24));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "junit", "junit", 25));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.hamcrest", "hamcrest-library", 26));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.assertj", "assertj-core", 27));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "com.google.code.findbugs", "jsr305", 28));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.jacoco", "org.jacoco.agent", 29));
        assertTrue(isDependencyContainedAndQueried(parsedDeps, "org.codehaus.plexus", "plexus-xml", 30));

        assertTrue(checkDependencyVersion(parsedDeps, "commons-compress", "1.23.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "commons-lang3", "3.12.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "commons-io", "2.12.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-reporting-api", "3.1.1"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-core", "3.2.5"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-plugin-api", "3.2.5"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-artifact", "3.2.5"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-model", "3.2.5"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-compat", "3.2.5"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-settings", "3.2.5"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-shared-utils", "3.3.4"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-reporting-impl", "3.2.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-common-artifact-filters", "3.1.1"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-plugin-testing-harness", "3.3.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "xmlunit-core", "2.9.1"));
        assertTrue(checkDependencyVersion(parsedDeps, "htmlunit", "2.70.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "jansi", "2.4.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-verifier", "1.8.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "plexus-java", "1.2.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "mockito-core", "2.28.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "powermock-core", "2.0.9"));
        assertTrue(checkDependencyVersion(parsedDeps, "powermock-module-junit4", "2.0.9"));
        assertTrue(checkDependencyVersion(parsedDeps, "powermock-api-mockito2", "2.0.9"));
        assertTrue(checkDependencyVersion(parsedDeps, "powermock-reflect", "2.0.9"));
        assertTrue(checkDependencyVersion(parsedDeps, "javassist", "3.29.0-GA"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit", "4.13.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "hamcrest-library", "1.3"));
        assertTrue(checkDependencyVersion(parsedDeps, "assertj-core", "3.24.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "jsr305", "3.0.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "org.jacoco.agent", "0.8.8"));
        assertTrue(checkDependencyVersion(parsedDeps, "plexus-xml", "3.0.0"));
    }

    /**
     * Verifies the version of a specific dependency in the parsed results.
     *
     * @param parsedDeps list of parsed Maven coordinates
     * @param artifactId artifact ID to search for
     * @param version expected version string
     *
     * @return true if the dependency has the expected version, false otherwise
     */
    private boolean checkDependencyVersion(ArrayList<MavenCoordinate> parsedDeps, String artifactId, String version) {
        return findDependency(parsedDeps, artifactId).getVersion().equals(version);
    }

    /**
     * Validates the presence and position of a specific dependency in the parsed results.
     *
     * @param parsedDeps list of parsed Maven coordinates
     * @param groupId expected group ID
     * @param artifactId expected artifact ID
     * @param depNumber expected position in the list (0-based index)
     *
     * @return true if the dependency is found at the specified position with matching coordinates
     */
    private boolean isDependencyContainedAndQueried(ArrayList<MavenCoordinate> parsedDeps,
                                          String groupId,
                                          String artifactId,
                                          Integer depNumber) {
        return parsedDeps.get(depNumber).getGroupId().equals(groupId) &&
                parsedDeps.get(depNumber).getArtifactId().equals(artifactId);
    }

    /**
     * Locates a dependency by artifact ID in the parsed results.
     *
     * @param parsedDeps list of parsed Maven coordinates
     * @param artifactId artifact ID to search for
     *
     * @return the first matching {@link MavenCoordinate}, or null if not found
     */
    private MavenCoordinate findDependency(ArrayList<MavenCoordinate> parsedDeps, String artifactId) {
        return parsedDeps.stream()
                .filter(dep -> dep.getArtifactId().equals(artifactId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Copies a POM file from test resources to the temporary test directory.
     * <p>
     * This helper method ensures test isolation by working with copies of resource files
     * in JUnit-managed temporary directories.
     *
     * @param sourcePom path to the source POM file in test resources
     * @param targetDir temporary directory for test execution
     * @throws RuntimeException if file copying fails
     */
    private void preparePom(String sourcePom, Path targetDir) {
        try {
            Path pathToPom = Path.of(sourcePom);
            Path target = targetDir.resolve(pathToPom.getFileName());
            Files.copy(pathToPom, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
