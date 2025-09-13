package org.altlinux.xgradle.parsers;

import com.google.inject.Inject;

import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.parsers.PomParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileInputStream;

import java.io.IOException;
import java.nio.file.Path;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcurrentPomParser implements PomParser {
    private static final Logger logger = LogManager.getLogger(ConcurrentPomParser.class);
    private final PomContainer pomContainer;

    @Inject
    public ConcurrentPomParser(PomContainer pomContainer){
        this.pomContainer = pomContainer;
    }

    @Override
    public HashMap<String, Path> getArtifactCoords(String searchingDir, Optional<String> artifactName) {
        Collection<Path> pomPaths;

        if (artifactName.isPresent()) {
            pomPaths = pomContainer.getSelectedPomPaths(searchingDir, artifactName.get());
        } else {
            pomPaths = pomContainer.getAllPomPaths(searchingDir);
        }
        int threadCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        ConcurrentHashMap<String, Path> artifactCoordinatesMap = new ConcurrentHashMap<>();

        try {
            CompletableFuture.allOf(pomPaths.stream()
                    .map(pomPath -> CompletableFuture.runAsync(() -> {
                        try {
                            Path jarPath = getJarPathFromPom(pomPath);
                            artifactCoordinatesMap.put(pomPath.toString(), jarPath);
                        } catch (Exception e) {
                            logger.error("Failed to parse: {}", pomPath.toString(), e);
                        }
                    }, executor)).toArray(CompletableFuture[]::new)).join();
        } finally {
            executor.shutdown();
        }
        return new HashMap<>(artifactCoordinatesMap);
    }

    private synchronized Path getJarPathFromPom(Path pomPath) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        String artifactId;
        String version;

        try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
            Model model = reader.read(fis);
            artifactId = model.getArtifactId();
            version = model.getVersion();

            if (version == null && model.getParent() != null) {
                version = model.getParent().getVersion();
            }

            if (artifactId == null || version == null) {
                throw new RuntimeException("Could not determine artifactId or version for POM: " + pomPath);
            }

            String jarFileName = artifactId + "-" + version + ".jar";

            return pomPath.getParent().resolve(jarFileName);

        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Error reading POM file: " + pomPath, e);
        }
    }
}