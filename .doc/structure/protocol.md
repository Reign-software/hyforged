# Protocol & Packets (com.hypixel.hytale.protocol)

## Core classes
- `PacketRegistry` — registers all network packets and their codecs.
- `Packet` — base packet type.
- `ProtocolSettings` — protocol version/hash and limits.

## Subpackages
- **io** — protocol IO utilities and validation.
- **packets** — packet definitions grouped by feature area.

## Notes for plugin developers
- Packet classes are grouped by feature (asset editor, auth, inventory, world, etc.).
- `PacketRegistry` lists all packet types in one file, which is useful for quick search.
