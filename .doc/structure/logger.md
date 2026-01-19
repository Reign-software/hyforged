# Logging (com.hypixel.hytale.logger)

## Core classes
- `HytaleLogger` — main logging API (Flogger‑based).
- `HytaleLoggerBackend` — backend, log levels, and sinks.
- `HytaleFileHandler` — file logging.
- `HytaleLogManager` — log manager integration.

## Bootstrap behavior
- `HytaleLogger.init()` initializes the logging backend.
- `HytaleLogger.replaceStd()` redirects `System.out` / `System.err` into logger streams.
- Sentry is configured during `HytaleServer` boot unless disabled or early plugins are active.
