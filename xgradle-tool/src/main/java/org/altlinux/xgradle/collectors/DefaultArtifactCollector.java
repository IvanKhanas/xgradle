package org.altlinux.xgradle.collectors;

import org.altlinux.xgradle.api.collectors.ArtifactCollector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import java.util.Map;
import java.util.stream.Stream;

public class DefaultArtifactCollector implements ArtifactCollector {
    private final Map<String, Path> ARTIFACTS_CACHE = new HashMap<>();

    @Override
    public HashMap<String,Path> collect(String searchingDirectory) {
      try (Stream<Path> paths = Files.walk(Path.of(searchingDirectory), Integer.MAX_VALUE)){
          paths.filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".jar"))
                  .forEach(path -> {
                      String filename = path.getFileName().toString();
                      String baseName = filename.substring(0, filename.lastIndexOf('.'));
                      String artifactName = baseName.replaceAll("-\\d+(\\.\\d+)*$", "");

                      ARTIFACTS_CACHE.put(artifactName, path);
                  });
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      return new HashMap<>(ARTIFACTS_CACHE);
    }
}
