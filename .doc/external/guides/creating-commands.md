# Creating Commands (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/creating-commands

## Purpose
Defines how to build server commands that players can execute.

## Key concepts
- Use `AbstractPlayerCommand` to create player‑facing commands.
- Commands run off the main thread (async), so avoid direct world mutations unless routed to server execution helpers.
- The `execute` method exposes `CommandContext`, `Store<EntityStore>`, `Ref<EntityStore>`, `PlayerRef`, and `World`.

## Arguments
- Use required or optional arguments when declaring command parameters.
- Common argument types include string, integer, boolean, float, double, and UUID.

## Registration
- Register commands in your plugin `setup()` via `getCommandRegistry().registerCommand(...)`.

## Related server areas
- Command system lives under `com.hypixel.hytale.server.core.command`.
- ECS data access comes from `EntityStore` components.
