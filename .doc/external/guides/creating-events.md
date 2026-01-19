# Creating Events (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/creating-events

## Purpose
Register handlers for server events and ECS events.

## Standard events
- Write a handler method that accepts an event type (e.g., player ready).
- Register via `getEventRegistry().registerGlobal(EventClass, handler)` in `setup()`.

## ECS events
- ECS events use `EntityEventSystem` implementations.
- Register ECS systems through `getEntityStoreRegistry().registerSystem(...)`.

## Related server areas
- Global events: `com.hypixel.hytale.event.*`
- ECS: `com.hypixel.hytale.component.*` and `EntityStore` in server core.
