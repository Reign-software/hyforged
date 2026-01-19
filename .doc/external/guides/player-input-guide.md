# Player Input Guide (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/player-input-guide

## Purpose
Handle player input by intercepting interaction packets.

## Key concepts
- Hytale servers receive interaction packets rather than raw key input.
- Use `SyncInteractionChains` to detect interaction types (Primary, Secondary, Use, SwapTo, etc.).
- Implement a packet watcher/filter to react to specific interaction types.

## Notes
- Useful in combination with the packet listening guide and client‑to‑server packet reference.
