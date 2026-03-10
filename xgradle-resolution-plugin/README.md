# xgradle-resolution-plugin

**xgradle-resolution-plugin** is the Gradle-side component of **xgradle** — an **offline-first toolkit** for Gradle builds.

It is implemented as a **Gradle plugin applied to the Gradle instance** (not to a single project), so it must be
loaded early via a Gradle **init script**. The module is distributed as:

- **`xgradle-resolution-plugin.jar`** — the plugin JAR
- **`xgradle-resolution-plugin.gradle`** — init script that adds the JAR to the init classpath and applies the plugin

---

## Where it fits in RPM packaging

In RPM (and similar distro packaging) workflows **xgradle-resolution-plugin** is typically used in the **`%build`** section.

It makes the *build itself* reproducible and offline-friendly by teaching Gradle to consume the **system/local artifact
set** (prepared by packaging) rather than downloading from the network.

---

## What it does

### 1) System dependency resolution (projects)
- Adds a **flatDir** repository for system JAR directories (scanned recursively).
- Uses Maven **POM metadata** from a system directory to drive resolution:
    - versions / BOM-managed versions
    - controlled transitive dependencies
    - substitutions / mapping to system artifacts

### 2) Local Gradle plugin resolution (Settings `pluginManagement`)
- Configures `pluginManagement.repositories` to include the same system JAR directories,
  allowing Gradle plugins to be resolved from local/system artifacts.

### 3) Optional SBOM generation
- If `generate.sbom` is set to `spdx` or `cyclonedx`, xgradle-resolution-plugin generates an SBOM report
  from resolved build artifacts.
- Report path:
  - `build/reports/xgradle/sbom-spdx.json`
  - `build/reports/xgradle/sbom-cyclonedx.json`

---

## Configuration

xgradle-resolution-plugin is configured via **system properties** or the user config file
`~/.xgradle/xgradle.config` (Java properties format). System properties set with
`-D` take precedence.

| Property | Meaning |
|---|---|
| `java.library.dir` | One or more directories containing **system JARs** (comma-separated). |
| `maven.poms.dir` | Directory containing **system Maven POM metadata**. |
| `disable.logo=true` | Disable ASCII banner printing. |
| `enable.ansi.color=true` | Enable ANSI colors in xgradle logs. |
| `xgradle.scan.depth` | Max directory scan depth for system artifacts (default `3`). |
| `generate.sbom` | SBOM format: `spdx` or `cyclonedx`. |

Example config file (`~/.xgradle/xgradle.config`):

```
java.library.dir=/usr/share/java,/usr/local/share/java
maven.poms.dir=/usr/share/maven-poms
disable.logo=true
enable.ansi.color=true
xgradle.scan.depth=3
generate.sbom=spdx
```

## Usage Example:

```bash
gradle build \
  -Djava.library.dir=/usr/share/java,/usr/local/share/java \
  -Dmaven.poms.dir=/usr/share/maven-poms \
  -Dgenerate.sbom=cyclonedx \
  --offline
```
