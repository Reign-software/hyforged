# Playing Sounds (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/playing-sounds

## Purpose
Play sound events to players from server plugins.

## Key concepts
- Sound IDs are looked up via the `SoundEvent` asset map.
- Sounds are executed through the world with a `TransformComponent` position.
- Use `SoundCategory` to classify playback (SFX, UI, Music, etc.).

## Typical flow
- Resolve the sound index.
- Get the player reference and world/store.
- Execute playback through the world execution context to ensure thread safety.

## Related server areas
- `com.hypixel.hytale.protocol` (sound event types)
- `com.hypixel.hytale.server.core` world and entity modules
