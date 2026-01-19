# Player Death Event (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/player-death-event

## Purpose
React to entity death via the ECS death system.

## Key concepts
- Extend `DeathSystems.OnDeathSystem`.
- Use `getQuery()` to filter for player entities.
- Handle `onComponentAdded` when `DeathComponent` is attached.
- Read death info (damage amount/source) from `DeathComponent`.

## Plugin integration
- Register the system using `getEntityStoreRegistry().registerSystem(...)`.
