package org.altlinux.xgradle.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.altlinux.xgradle.api.services.PomService;
import org.altlinux.xgradle.api.redactors.ParentRedactor;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for POM file processing operations.
 * Provides methods for artifact filtering, parent block removal, and snapshot exclusion.
 * Implements PomService interface for POM file manipulation and filtering.
 *
 * @author Ivan Khanas
 */
@Singleton
public class PomProcessingService implements PomService {
    private final ParentRedactor parentRedactor;

    /**
     * Constructs a new PomProcessingService with required dependencies.
     *
     * @param parentRedactor redactor for removing parent blocks from POM files
     */
    @Inject
    public PomProcessingService(@Named("Remove")ParentRedactor parentRedactor) {
        this.parentRedactor = parentRedactor;
    }

    /**
     * Excludes artifacts from HashMap based on exclusion patterns.
     * Filters out artifacts whose file names start with any of the excluded patterns.
     *
     * @param excludedArtifacts list of artifact patterns to exclude
     * @param artifactCoordinatesMap map of artifact coordinates to filter
     * @return filtered map of artifact coordinates
     */
    @Override
    public HashMap<String, Path> excludeArtifacts(List<String> excludedArtifacts, HashMap<String, Path> artifactCoordinatesMap) {
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            return artifactCoordinatesMap.entrySet().stream()
                    .filter(entry -> {
                        Path pomPath = Path.of(entry.getKey());
                        String filename = pomPath.getFileName().toString();
                        return excludedArtifacts.stream()
                                .noneMatch(filename::startsWith);
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (existing, replacement) -> existing,
                            HashMap::new
                    ));
        } else {
            return artifactCoordinatesMap;
        }
    }

    /**
     * Excludes artifacts from Set based on exclusion patterns.
     * Filters out artifacts whose file names start with any of the excluded patterns.
     *
     * @param excludedArtifacts list of artifact patterns to exclude
     * @param artifactCoords set of artifact paths to filter
     * @return filtered set of artifact paths
     */
    @Override
    public Set<Path> excludeArtifacts(List<String> excludedArtifacts, Set<Path> artifactCoords) {
        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            return artifactCoords.stream()
                    .filter(path -> excludedArtifacts.stream()
                            .noneMatch(path.getFileName().toString()::startsWith))
                    .collect(Collectors.toSet());
        }
        return artifactCoords;
    }

    /**
     * Removes parent blocks from artifacts in HashMap.
     * Processes POM files and removes parent blocks based on specified patterns.
     *
     * @param artifacts map of artifacts to process
     * @param removeParentPoms list of POM patterns for which to remove parent blocks
     */
    @Override
    public void removeParentBlocks(HashMap<String, Path> artifacts, List<String> removeParentPoms) {
        if (removeParentPoms == null || removeParentPoms.isEmpty()) {
            return;
        }
        artifacts.forEach((pom, jar) -> {
            Path pomPath = Path.of(pom);
            String filename = pomPath.getFileName().toString();

            boolean shouldRemove = removeParentPoms.contains("all") ||
                    removeParentPoms.stream().anyMatch(filename::startsWith);

            if (shouldRemove) {
                parentRedactor.removeParent(pomPath);
            }
        });
    }

    /**
     * Removes parent blocks from BOM files in Set.
     * Processes BOM files and removes parent blocks based on specified patterns.
     *
     * @param bomFiles set of BOM files to process
     * @param removeParentPoms list of POM patterns for which to remove parent blocks
     */
    @Override
    public void removeParentBlocks(Set<Path> bomFiles, List<String> removeParentPoms) {
        if (removeParentPoms == null || removeParentPoms.isEmpty()) {
            return;
        }

        bomFiles.forEach(bomFile -> {
            String fileName = bomFile.getFileName().toString();
            boolean shouldRemove = removeParentPoms.contains("all") ||
                    removeParentPoms.stream().anyMatch(fileName::startsWith);

            if (shouldRemove) {
                parentRedactor.removeParent(bomFile);
            }
        });
    }

    /**
     * Excludes snapshot artifacts from Set.
     * Filters out POM files that are snapshot versions.
     *
     * @param pomFiles set of POM files to filter
     * @return filtered set without snapshot artifacts
     */
    @Override
    public Set<Path> excludeSnapshots(Set<Path> pomFiles) {
        return pomFiles.stream()
                .filter(pomPath -> !isSnapshotPom(pomPath))
                .collect(Collectors.toSet());
    }

    /**
     * Excludes snapshot artifacts from HashMap.
     * Filters out artifacts that are snapshot versions.
     *
     * @param artifactsMap map of artifacts to filter
     * @return filtered map without snapshot artifacts
     */
    @Override
    public HashMap<String, Path> excludeSnapshots(HashMap<String, Path> artifactsMap) {
        return artifactsMap.entrySet().stream().
                filter (entry -> {
                    Path pomPath = Path.of(entry.getKey());

                    return !isSnapshotPom(pomPath);
                }).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        HashMap::new
                ));
    }

    /**
     * Checks if a POM file represents a snapshot artifact.
     * Examines both the filename and the version in the POM model.
     *
     * @param pomPath path to the POM file to check
     * @return true if the artifact is a snapshot, false otherwise
     */
    private boolean isSnapshotPom(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;

        if (pomPath.getFileName().toString().toLowerCase().contains("snapshot")) {
            return true;
        }

        try {
            File pomFile = new File(String.valueOf(pomPath));
            model = reader.read(new FileReader(pomFile));

            if (model.getVersion() != null && model.getVersion().toLowerCase().contains("snapshot")) {
                return true;
            }

        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Unable to initialize POM model for:" + pomPath, e);
        }
        return false;
    }
}