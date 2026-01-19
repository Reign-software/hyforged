# Setting Up Your Development Environment (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/setting-up-env

## Purpose
Install tooling for Hytale plugin development.

## Key steps
- Install Java 25+ (OpenJDK recommended).
- Install an IDE (IntelliJ IDEA is the suggested default).
- Install Maven and ensure it is on PATH.
- Clone the plugin template repo and open it as a Maven project.
- Add `HytaleServer.jar` as a library.
- Install the server jar into the local Maven repository (the guide notes PowerShell quoting issues with Maven parameters).

## Related server areas
- Plugin template config uses `pom.xml` and `manifest.json`.
