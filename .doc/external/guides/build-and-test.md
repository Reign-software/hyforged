# Build and Test Your Mod (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/build-and-test

## Purpose
Package your plugin and verify it loads in Hytale.

## Build flow
- Build with Maven (creates a JAR in `target/`).
- The output name is based on the `artifactId` and `version` in `pom.xml`.

## Test flow
- Copy the plugin JAR into the Hytale Mods folder.
- Launch Hytale and confirm the mod appears in the Mods list.

## Troubleshooting
- Verify mod location, `manifest.json` correctness, and Java/Maven versions.
