# Customizing Hotbar Actions (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/customizing-hotbar-actions

## Purpose
Intercept hotbar slot switching to implement custom ability triggers.

## Key concepts
- Detect `SyncInteractionChains` packets (interaction type SwapFrom and target slot).
- Use a `PlayerPacketFilter` to block the slot switch and trigger custom logic.
- Fix client/server desync by sending `SetActiveSlot` and updating server‑side inventory state.
- Run entity changes on the world thread via `world.execute()`.

## Typical flow
1. Register a packet filter in `setup()` and keep a handle for deregistration.
2. In `test()`, look for `SyncInteractionChains` and target slot index.
3. Block the packet, trigger ability logic, and force the client back to the original slot.

## Notes
- Packet handlers run on network threads; always switch to world thread for entity mutations.
