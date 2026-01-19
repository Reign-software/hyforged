# Event System Flow

## Server event bus
- `HytaleServer` creates a single `EventBus` instance.
- The bus uses either synchronous or asynchronous registries depending on event type.
- Event timing can be enabled by `--event-debug` (`Options.EVENT_DEBUG`).

## Registry types
- `SyncEventBusRegistry` for `IEvent` events.
- `AsyncEventBusRegistry` for `IAsyncEvent` events.
- Registries support priorities, keyed listeners, and global/unhandled listeners.

## Plugin‑side registration
`PluginBase` exposes an `EventRegistry` that is backed by the server `EventBus`.
- Use the plugin’s `EventRegistry` for lifecycle‑bound registration.
- Registrations are automatically cleaned up during plugin shutdown.

## Typical flow
1. Plugin registers listeners during `setup()`.
2. Server (or another system) dispatches events via the server `EventBus`.
3. `EventBusRegistry` routes events to registered consumers based on priority and optional keys.

## Relevant classes
- `com.hypixel.hytale.event.EventBus`
- `com.hypixel.hytale.event.EventBusRegistry`
- `com.hypixel.hytale.event.EventRegistration`
- `com.hypixel.hytale.event.EventPriority`
- `com.hypixel.hytale.server.core.plugin.EventRegistry`
