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

import org.gradle.api.logging.Logging;
import org.gradle.api.logging.Logger;

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
 * Integration tests for validating the parsing of dependencies sections in Maven POM files.
 * <p>
 * This test class verifies the functionality of the {@link DefaultPomParser#parseDependencies(Path, Logger)}
 * method. It ensures that the parser correctly extracts and processes dependency information from the
 * {@code <dependencies>} section of POM files, including:
 * <ul>
 *   <li>Identification of all declared dependencies</li>
 *   <li>Correct resolution of groupId, artifactId, and version</li>
 *   <li>Proper handling of dependency scopes (compile, provided, test, etc.)</li>
 *   <li>Accurate inheritance and resolution of properties</li>
 *   <li>Correct processing of transitive dependency hierarchies</li>
 * </ul>
 *
 * <p>Test scenarios cover various types of POM files:
 * <ol>
 *   <li>Complex projects with multiple dependencies</li>
 *   <li>Simple utility libraries</li>
 *   <li>Framework modules</li>
 *   <li>Low-level libraries</li>
 *   <li>Projects with deep dependency hierarchies (Maven Wagon)</li>
 * </ol>
 *
 * <p>Each test method focuses on specific aspects of dependency parsing:
 * <ul>
 *   <li>Verification of dependency count and order</li>
 *   <li>Validation of artifact coordinates (groupId, artifactId)</li>
 *   <li>Checking resolved versions against expected values</li>
 *   <li>Validation of scope assignment</li>
 *   <li>Handling of dependency inheritance and BOM influences</li>
 * </ul>
 *
 * @see DefaultPomParser
 * @see DefaultPomParser#parseDependencies(Path, Logger)
 *
 * @author Ivan Khanas
 */
public class TestDependenciesBlockParser {
    private DefaultPomParser pomParser;
    private ArrayList<MavenCoordinate> parsedDeps;

    Logger logger;

    /**
     * Initializes test environment before each test method execution.
     * <p>
     * Creates a new instance of {@link DefaultPomParser} and configures a logger
     * for the test class. This ensures test isolation and consistent initial
     * conditions for all test cases.
     */
    @BeforeEach
    public void prepareParser() {
        pomParser = new DefaultPomParser();
        logger = Logging.getLogger(this.getClass());
    }

    /**
     * Tests parsing of the Maven Surefire Common POM file.
     * <p>
     * Validates parser behavior with a complex project containing multiple dependencies
     * of different scopes. The test verifies:
     * <ul>
     *   <li>Order and presence of specific dependencies</li>
     *   <li>Correct scope assignment (compile, provided, test)</li>
     *   <li>Presence of dependencies from various groups</li>
     *   <li>Version resolution accuracy</li>
     *   <li>Total dependency count</li>
     * </ul>
     *
     * @param tempDir temporary directory provided by JUnit for test file operations
     */
    @Test
    public void parseMavenSurefireCommonsPom (@TempDir Path tempDir) {
        preparePom("test/resources/poms/maven-surefire/maven-surefire-common.pom", tempDir);
        preparePom("test/resources/poms/maven-surefire/surefire.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("maven-surefire-common.pom")), logger);

        assertFalse(parsedDeps.isEmpty());

        assertEquals("apiguardian-api", parsedDeps.get(0).getArtifactId());
        assertEquals("surefire-api" ,parsedDeps.get(4).getArtifactId());
        assertEquals("plexus-xml", parsedDeps.get(parsedDeps.size()-1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "surefire-api", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "maven-core", "provided"));
        assertTrue(checkDependencyScope(parsedDeps, "powermock-module-junit4", "test"));

        assertTrue(isDependencyContained(parsedDeps, "org.codehaus.plexus", "plexus-java"));
        assertTrue(isDependencyContained(parsedDeps, "org.mockito", "mockito-core"));
        assertTrue(isDependencyContained(parsedDeps, "org.apache.maven.surefire", "surefire-shared-utils"));

        assertTrue(checkDependencyVersion(parsedDeps, "surefire-booter", "3.2.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "maven-common-artifact-filters", "3.1.1"));
        assertTrue(checkDependencyVersion(parsedDeps, "jansi", "2.4.0"));

        assertEquals(20, parsedDeps.size());
    }

    /**
     * Tests parsing of the Plexus XML POM file.
     * <p>
     * Verifies parser behavior with a simple utility library. The test checks:
     * <ul>
     *   <li>Dependency order and completeness</li>
     *   <li>All dependencies marked as test scope</li>
     *   <li>Correct version resolution</li>
     *   <li>Small dependency count (simple project)</li>
     * </ul>
     *
     * @param tempDir temporary directory provided by JUnit for test file operations
     */
    @Test
    public void parsePlexusXmlPom (@TempDir Path tempDir) {
        preparePom("test/resources/poms/plexus-xml/plexus-xml.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("plexus-xml.pom")), logger);

        assertFalse(parsedDeps.isEmpty());

        assertEquals("plexus-utils", parsedDeps.get(0).getArtifactId());
        assertEquals("jmh-core", parsedDeps.get(1).getArtifactId());
        assertEquals("jmh-generator-annprocess", parsedDeps.get(2).getArtifactId());
        assertEquals("junit", parsedDeps.get(parsedDeps.size()-1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "plexus-utils", "test"));
        assertTrue(checkDependencyScope(parsedDeps, "jmh-core", "test"));
        assertTrue(checkDependencyScope(parsedDeps, "jmh-generator-annprocess", "test"));
        assertTrue(checkDependencyScope(parsedDeps, "junit", "test"));

        assertTrue(checkDependencyVersion(parsedDeps, "plexus-utils", "4.0.0"));
        assertTrue(checkDependencyVersion(parsedDeps, "jmh-core", "1.36"));
        assertTrue(checkDependencyVersion(parsedDeps, "jmh-generator-annprocess", "1.36"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit", "4.13.2"));

        assertEquals(4, parsedDeps.size());
    }

    /**
     * Tests parsing of the JUnit Jupiter POM file.
     * <p>
     * Validates parser behavior with a framework module. The test verifies:
     * <ul>
     *   <li>Correct ordering of internal module dependencies</li>
     *   <li>All dependencies marked as compile scope</li>
     *   <li>Consistent version across related artifacts</li>
     *   <li>Small dependency count (focused module)</li>
     * </ul>
     *
     * @param tempDir temporary directory provided by JUnit for test file operations
     */
    @Test
    public void parseJunitJupiterPom (@TempDir Path tempDir) {
        preparePom("test/resources/poms/junit5/junit-jupiter.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("junit-jupiter.pom")), logger);

        assertFalse(parsedDeps.isEmpty());

        assertEquals("junit-jupiter-api", parsedDeps.get(0).getArtifactId());
        assertEquals("junit-jupiter-params", parsedDeps.get(1).getArtifactId());
        assertEquals("junit-jupiter-engine", parsedDeps.get(parsedDeps.size()-1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "junit-jupiter-api", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "junit-jupiter-params", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "junit-jupiter-engine", "compile"));

        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-api", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-params", "5.8.2"));
        assertTrue(checkDependencyVersion(parsedDeps, "junit-jupiter-engine", "5.8.2"));

        assertEquals(3, parsedDeps.size());
    }

    /**
     * Tests parsing of the ASM Commons POM file.
     * <p>
     * Verifies parser behavior with a low-level bytecode manipulation library. The test checks:
     * <ul>
     *   <li>Minimal dependency list</li>
     *   <li>Correct compile scope assignment</li>
     *   <li>Version consistency across related artifacts</li>
     *   <li>Simple dependency hierarchy</li>
     * </ul>
     *
     * @param tempDir temporary directory provided by JUnit for test file operations
     */
    @Test
    public void parseAsmCommonsPom(@TempDir Path tempDir) {
        preparePom("test/resources/poms/asm/asm-commons.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("asm-commons.pom")), logger);

        assertFalse(parsedDeps.isEmpty());

        assertEquals("asm", parsedDeps.get(0).getArtifactId());
        assertEquals("asm-tree", parsedDeps.get(parsedDeps.size()-1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "asm", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "asm-tree", "compile"));

        assertTrue(checkDependencyVersion(parsedDeps, "asm", "9.8"));
        assertTrue(checkDependencyVersion(parsedDeps, "asm-tree", "9.8"));

        assertEquals(2, parsedDeps.size());
    }

    /**
     * Tests parsing of the Maven Wagon HTTP POM file.
     * <p>
     * Validates parser behavior with a project that has deep dependency hierarchies.
     * The test verifies:
     * <ul>
     *   <li>Complex dependency resolution with parent POMs</li>
     *   <li>Mixed scope assignments (compile, runtime, test)</li>
     *   <li>Correct ordering of dependencies</li>
     *   <li>Handling of transitive dependency resolution</li>
     *   <li>Version accuracy across multiple artifact types</li>
     * </ul>
     *
     * @param tempDir temporary directory provided by JUnit for test file operations
     */
    @Test
    public void parseMavenWagonHttpPom(@TempDir Path tempDir) {
        preparePom("test/resources/poms/maven-wagon/wagon-providers.pom", tempDir);
        preparePom("test/resources/poms/maven-wagon/wagon-http.pom", tempDir);
        preparePom("test/resources/poms/maven-wagon/wagon.pom", tempDir);

        parsedDeps = pomParser.parseDependencies(tempDir.resolve(Path.of("wagon-http.pom")), logger);

        assertFalse(parsedDeps.isEmpty());

        assertEquals("wagon-provider-api", parsedDeps.get(0).getArtifactId());
        assertEquals("wagon-http-shared", parsedDeps.get(1).getArtifactId());
        assertEquals("javax.servlet-api", parsedDeps.get(parsedDeps.size()-2).getArtifactId());
        assertEquals("plexus-container-default", parsedDeps.get(parsedDeps.size()-1).getArtifactId());

        assertTrue(checkDependencyScope(parsedDeps, "wagon-http-shared", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "httpclient", "compile"));
        assertTrue(checkDependencyScope(parsedDeps, "jcl-over-slf4j", "runtime"));
        assertTrue(checkDependencyScope(parsedDeps, "slf4j-api", "test"));

        assertTrue(checkDependencyVersion(parsedDeps, "wagon-http-shared", "3.5.3"));
        assertTrue(checkDependencyVersion(parsedDeps, "jcl-over-slf4j", "1.7.36"));
        assertTrue(checkDependencyVersion(parsedDeps, "jetty-all", "9.2.30.v20200428"));
        assertTrue(checkDependencyVersion(parsedDeps, "plexus-container-default", "2.1.0"));

        assertEquals(10 ,parsedDeps.size());
    }

    /**
     * Verifies the scope of a specific dependency in the parsed results.
     *
     * @param parsedDeps list of parsed Maven coordinates
     * @param artifactId artifact ID to search for
     * @param scope expected scope
     *
     * @return true if the dependency has the expected scope, false otherwise
     */
    private boolean checkDependencyScope(ArrayList<MavenCoordinate> parsedDeps, String artifactId, String scope) {
        return findDependency(parsedDeps, artifactId).getScope().equals(scope);
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
     * Checks if a dependency with specific coordinates exists in the parsed results.
     *
     * @param parsedDeps list of parsed Maven coordinates
     * @param groupId expected group ID
     * @param artifactId expected artifact ID
     *
     * @return true if a matching dependency is found, false otherwise
     */
    private boolean isDependencyContained(ArrayList<MavenCoordinate> parsedDeps, String groupId, String artifactId) {
        return parsedDeps.stream()
                .anyMatch(dep -> dep.getArtifactId().equals(artifactId) && dep.getGroupId().equals(groupId));
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
     * in JUnit-managed temporary directories. It handles:
     * <ul>
     *   <li>Copying single POM files</li>
     *   <li>Copying related POM files for dependency resolution</li>
     *   <li>Preserving file names during copy</li>
     * </ul>
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
