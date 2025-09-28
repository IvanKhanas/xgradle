package org.altlinux.xgradle.parsers;

import com.google.inject.Inject;
import org.altlinux.xgradle.api.containers.PomContainer;
import org.altlinux.xgradle.api.parsers.PomParser;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class DefaultBomParser implements PomParser<Set<Path>> {
    private static final Logger logger = LoggerFactory.getLogger("XgradleLogger");
    private final PomContainer pomContainer;

    @Inject
    public DefaultBomParser(PomContainer pomContainer) {
        this.pomContainer = pomContainer;
    }

    @Override
    public DefaultBomParser parsePoms(){
        return this;
    }

    @Override
    public Set<Path> getArtifactCoords(String searchingDir, Optional<String> artifactName) {
        HashMap<String, Path> artifactCoordsMap;

        if (artifactName.isPresent()) {
            artifactCoordsMap = pomContainer.getSelectedPoms(searchingDir, String.valueOf(artifactName));
        } else {
            artifactCoordsMap = pomContainer.getAllPoms(searchingDir);
        }

        Set<Path> artifactCoords = artifactCoordsMap.values().stream()
                .filter(pomPath -> pomPath.getFileName().toString().toLowerCase().contains("bom"))
                .filter(pomPath -> {
                    MavenXpp3Reader reader = new MavenXpp3Reader();
                    try (FileInputStream fis = new FileInputStream(pomPath.toFile())) {
                        Model model = reader.read(fis);

                        if ("pom".equals(model.getPackaging()) && model.getDependencyManagement() != null) {
                            return true;
                        }
                    } catch (IOException | XmlPullParserException e) {
                        throw new RuntimeException(e);
                    }
                    return false;
                })
                .collect(Collectors.toSet());
        return artifactCoords;
    }
}
