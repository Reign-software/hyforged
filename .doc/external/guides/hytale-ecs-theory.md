# Hytale ECS Theory (Summary)

Source: https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory

## Purpose
Explain core ECS concepts used by the Hytale server.

## Key concepts
- `Store` is the ECS container for entities and components.
- `EntityStore` is world‑aware and indexes entities by UUID and network ID.
- `ChunkStore` holds chunk and block component data.
- `Holder` is a pre‑entity blueprint used during construction.
- `Ref` is a safe entity handle; never keep raw entity references.
- Components are data‑only and are retrieved via `ComponentType`.
- `CommandBuffer` queues ECS changes safely.

## Player components
- `PlayerRef` represents the connected player identity.
- `Player` represents the in‑world entity presence.

## Plugin relevance
- Most server APIs surface `Store<EntityStore>` and `Ref<EntityStore>` for safe access.
