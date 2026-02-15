
# xgradle-tool

**xgradle-tool** is the **standalone CLI** component of **xgradle**.

It is responsible for handling the **artifacts produced by a build that runs with xgradle-core**:
- discovers build outputs (JARs + POM metadata, optional javadocs),
- installs/exports them into packaging-controlled directories,
- registers artifacts and BOMs (typically via **XMvn** tooling),
- optionally **patches (redacts)** POM files to match offline/system packaging requirements.

---

## Where it fits in RPM packaging

In RPM (and similar distro packaging) workflows **xgradle-tool** is typically used in the **`%install`** section.

Its job is to take the artifacts produced by the build and **prepare them for distribution**:
place them into the correct filesystem layout, generate/adjust metadata as needed, and ensure the resulting
system artifact set is ready to be consumed offline.

---

## Artifacts

This module produces:

- `xgradle-tool.jar` — runnable CLI JAR
- `xgradle-tool` — launcher script/wrapper

---

## Quick start

Show help:

```bash
xgradle-tool --help
