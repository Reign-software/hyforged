# Listening to Packets (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/listening-to-packets

## Purpose
Observe or filter inbound/outbound network packets from plugins.

## Key concepts
- Packets live in `com.hypixel.hytale.protocol` and `com.hypixel.hytale.protocol.packets`.
- Server packet processing is routed through adapter hooks.
- Use `PacketAdapters.registerInbound(...)` and `registerOutbound(...)` to attach watchers or filters.
- Filters can block packets from reaching the server or client.

## Notes
- Packet handlers are grouped by phase (initial, handshake, setup, game), with sub‑handlers for features like asset editor and inventory.
- This is safer than hooking Netty directly and is the recommended approach.
