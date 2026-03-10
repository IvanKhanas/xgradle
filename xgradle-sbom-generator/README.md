# xgradle-sbom-generator

**xgradle-sbom-generator** is a support module for generating SBOM reports from
resolved Gradle dependencies in offline/system-artifact workflows.

## Supported formats

- SPDX JSON
- CycloneDX JSON

## Activation

The module is consumed by `xgradle-resolution-plugin` and is activated by property:

```bash
-Dgenerate.sbom=spdx
-Dgenerate.sbom=cyclonedx
```

You can also put this key in `~/.xgradle/xgradle.config`.

## Output

Generated file is written to:

- `build/reports/xgradle/sbom-spdx.json`
- `build/reports/xgradle/sbom-cyclonedx.json`
