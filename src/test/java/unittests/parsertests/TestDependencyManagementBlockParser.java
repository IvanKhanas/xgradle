package unittests.parsertests;

import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.DefaultPomParser;

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

public class TestDependencyManagementBlockParser {
    private DefaultPomParser pomParser;
    private ArrayList<MavenCoordinate> parsedDeps;

    Logger logger;

    @BeforeEach
    public void prepareParser() {
        pomParser = new DefaultPomParser();
        logger = Logging.getLogger(this.getClass());
    }

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

    private boolean checkDependencyVersion(ArrayList<MavenCoordinate> parsedDeps, String artifactId, String version) {
        return findDependency(parsedDeps, artifactId).getVersion().equals(version);
    }

    private boolean isDependencyContainedAndQueried(ArrayList<MavenCoordinate> parsedDeps,
                                          String groupId,
                                          String artifactId,
                                          Integer depNumber) {
        return parsedDeps.get(depNumber).getGroupId().equals(groupId) &&
                parsedDeps.get(depNumber).getArtifactId().equals(artifactId);
    }

    private MavenCoordinate findDependency(ArrayList<MavenCoordinate> parsedDeps, String artifactId) {
        return parsedDeps.stream()
                .filter(dep -> dep.getArtifactId().equals(artifactId))
                .findFirst()
                .orElse(null);
    }

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
