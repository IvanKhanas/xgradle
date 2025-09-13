package org.altlinux.xgradle.collectors;

import org.altlinux.xgradle.api.collectors.PomCollector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DefaultPomCollector implements PomCollector {
    private final Map<String, Path> POM_CACHE = new HashMap<>();

    @Override
    public HashMap<String, Path> collectAll(String searchingDir) {
        try (Stream<Path> paths = Files.walk(Path.of(searchingDir), Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pom"))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                        String artifactName = baseName.replaceAll("-\\d+(\\.\\d+)*$", "");

                        POM_CACHE.put(artifactName, path);
                    });
        } catch (IOException e) {
              throw new RuntimeException(e);
        }
        return new HashMap<>(POM_CACHE);
    }

    @Override
    public HashMap<String, Path> collectSelected(String searchingDir, String artifactName) {
        try (Stream<Path> paths = Files.walk(Path.of(searchingDir) , Integer.MAX_VALUE)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pom"))
                    .filter(path -> path.toString().startsWith(artifactName))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                        String artifactId = baseName.replaceAll("-\\d+(\\.\\d+)*$", "");

                        POM_CACHE.put(artifactId, path);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new HashMap<>(POM_CACHE);
    }
}
