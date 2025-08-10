package org.altlinux.gradlePlugin.core.processors;

import org.altlinux.gradlePlugin.core.managers.BomDependencyManager;
import org.altlinux.gradlePlugin.services.PomFinder;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import java.util.*;

public class BomProcessor {
    private final Map<String, List<String>> bomManagedDeps = new LinkedHashMap<>();
    private final Set<String> processedBoms = new HashSet<>();
    private final Map<String, Boolean> testContextFlags;

    public BomProcessor(Map<String, Boolean> testContextFlags) {
        this.testContextFlags = testContextFlags;
    }

    public Set<String> process(Set<String> projectDependencies, PomFinder pomFinder, Logger logger) {
        BomDependencyManager bomManager = new BomDependencyManager(pomFinder, logger, testContextFlags);
        Set<String> allDependencies = bomManager.processBomDependencies(projectDependencies);
        processedBoms.addAll(bomManager.getProcessedBoms());
        bomManagedDeps.putAll(bomManager.getBomManagedDeps());
        return allDependencies;
    }

    public void removeBomsFromConfigurations(Gradle gradle) {
        gradle.allprojects(p -> p.getConfigurations().all(cfg -> {
            List<Dependency> toRemove = new ArrayList<>();
            for (Dependency d : cfg.getDependencies()) {
                String key = d.getGroup() + ":" + d.getName();
                if (processedBoms.contains(key)) toRemove.add(d);
            }
            toRemove.forEach(cfg.getDependencies()::remove);
        }));
    }

    public Map<String, List<String>> getBomManagedDeps() {
        return bomManagedDeps;
    }

    public Set<String> getManagedDependencies() {
        return new HashSet<>(bomManagedDeps.keySet());
    }
}