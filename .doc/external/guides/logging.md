# Logging (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/logging

## Purpose
Use the server logging system in plugins.

## Key concepts
- Use `HytaleLogger` for plugin logs.
- Logger names can be custom or use `HytaleLogger.forEnclosingClass()`.
- Logging levels include Info, Warning, and Severe.
- Template strings use printf‑style formatting.
- Attach exceptions with `withCause(...)` for stack traces.

## Related server areas
- Logging classes live under `com.hypixel.hytale.logger`.
