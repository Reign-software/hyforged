# Configuration & Options

## CLI options
- `Options` defines the CLI surface (`--assets`, `--mods`, `--event-debug`, etc.).
- `Options.parse(args)` handles parsing and log‑level setup.

## Server config
- `HytaleServerConfig` models `config.json` and uses a `BuilderCodec`.
- `HytaleServer` loads config on boot and periodically saves changes.
- Mod/plugin configuration is stored under `Mods` in the config.

## Related classes
- `HytaleServerConfig`
- `Options`
- `HytaleServer`
