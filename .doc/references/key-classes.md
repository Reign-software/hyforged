# Key Classes Index

## Entry + bootstrap
- `com.hypixel.hytale.Main` — JVM entry point and early plugin check.
- `com.hypixel.hytale.LateMain` — option parsing + server creation.
- `com.hypixel.hytale.plugin.early.EarlyPluginLoader` — early plugin scanning.
- `com.hypixel.hytale.plugin.early.TransformingClassLoader` — bytecode transform loader.

## Server core
- `com.hypixel.hytale.server.core.HytaleServer` — main server lifecycle.
- `com.hypixel.hytale.server.core.Options` — CLI options.
- `com.hypixel.hytale.server.core.HytaleServerConfig` — config model.

## Plugins
- `com.hypixel.hytale.server.core.plugin.PluginManager` — discovery and lifecycle.
- `com.hypixel.hytale.server.core.plugin.PluginBase` — plugin base class.
- `com.hypixel.hytale.server.core.plugin.JavaPlugin` — Java plugin subclass.
- `com.hypixel.hytale.common.plugin.PluginManifest` — plugin metadata.

## Events
- `com.hypixel.hytale.event.EventBus` — server event bus.
- `com.hypixel.hytale.server.core.plugin.EventRegistry` — plugin‑scoped registrations.

## Assets
- `com.hypixel.hytale.assetstore.AssetStore` — central asset access.
- `com.hypixel.hytale.assetstore.AssetRegistry` — asset registry.

## Protocol
- `com.hypixel.hytale.protocol.PacketRegistry` — packet registry.
- `com.hypixel.hytale.protocol.ProtocolSettings` — protocol metadata.

## Logging
- `com.hypixel.hytale.logger.HytaleLogger` — logging entry point.
- `com.hypixel.hytale.logger.backend.HytaleLoggerBackend` — backend + levels.

## UI System
- `com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud` — base class for HUDs.
- `com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager` — manages player HUD state.
- `com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage` — base class for UI pages.
- `com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage<T>` — interactive pages with event data.
- `com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager` — manages player page state.
- `com.hypixel.hytale.server.core.ui.builder.UICommandBuilder` — builds UI commands (append, set, clear).
- `com.hypixel.hytale.server.core.ui.builder.UIEventBuilder` — builds event bindings.
- `com.hypixel.hytale.server.core.ui.builder.EventData` — creates event data mappings.
- `com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType` — UI event types enum.
- `com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime` — page lifetime/closing behavior enum.

