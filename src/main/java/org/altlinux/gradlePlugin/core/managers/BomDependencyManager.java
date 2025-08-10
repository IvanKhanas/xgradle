// Файл: ./src/main/java/org/altlinux/gradlePlugin/core/managers/BomDependencyManager.java
package org.altlinux.gradlePlugin.core.managers;

import org.altlinux.gradlePlugin.model.MavenCoordinate;
import org.altlinux.gradlePlugin.services.PomFinder;
import org.gradle.api.logging.Logger;
import java.util.*;

public class BomDependencyManager {
    private final PomFinder pomFinder;
    private final Logger logger;
    private final Map<String, Boolean> testContextFlags;
    private final Set<String> processedBoms = new HashSet<>();
    private final Set<String> managedDependencies = new HashSet<>();
    private final Map<String, List<String>> bomManagedDeps = new LinkedHashMap<>();
    private final Map<String, Boolean> bomTestContext = new HashMap<>();

    public BomDependencyManager(PomFinder pomFinder, Logger logger, Map<String, Boolean> testContextFlags) {
        this.pomFinder = pomFinder;
        this.logger = logger;
        this.testContextFlags = testContextFlags;
    }

    public Set<String> processBomDependencies(Set<String> projectDependencies) {
        managedDependencies.clear();
        Set<String> allDependencies = new HashSet<>(projectDependencies);
        Queue<MavenCoordinate> bomQueue = new LinkedList<>();

        for (String dep : projectDependencies) {
            String[] parts = dep.split(":");
            if (parts.length < 2) continue;

            MavenCoordinate coord = pomFinder.findPomForArtifact(parts[0], parts[1], logger);
            if (coord != null && coord.isBom()) {
                bomQueue.add(coord);
                String bomKey = coord.getGroupId() + ":" + coord.getArtifactId();
                processedBoms.add(bomKey);
                bomTestContext.put(bomKey, testContextFlags.getOrDefault(dep, false));
            }
        }

        while (!bomQueue.isEmpty()) {
            MavenCoordinate bom = bomQueue.poll();
            String bomKey = bom.getGroupId() + ":" + bom.getArtifactId() + ":" + bom.getVersion();
            List<String> managedDeps = new ArrayList<>();
            boolean isTestBom = bomTestContext.get(bomKey.split(":")[0] + ":" + bomKey.split(":")[1]);

            List<MavenCoordinate> dependencies = pomFinder.getPomParser()
                    .parseDependencyManagement(bom.getPomPath(), logger);

            for (MavenCoordinate dep : dependencies) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                managedDeps.add(depKey + ":" + dep.getVersion());
                managedDependencies.add(depKey);

                if (isTestBom) {
                    testContextFlags.put(depKey, true);
                }

                if (dep.isBom() && !processedBoms.contains(depKey)) {
                    bomQueue.add(dep);
                    processedBoms.add(depKey);
                    bomTestContext.put(depKey, isTestBom);
                }

                if (!allDependencies.contains(depKey)) {
                    allDependencies.add(depKey);
                }
            }
            bomManagedDeps.put(bomKey, managedDeps);
        }
        return allDependencies;
    }

    public Map<String, List<String>> getBomManagedDeps() {
        return bomManagedDeps;
    }

    public Set<String> getProcessedBoms() {
        return processedBoms;
    }

    public Set<String> getManagedDependencies() {
        return managedDependencies;
    }
}
