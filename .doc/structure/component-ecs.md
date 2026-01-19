# Component/ECS System (com.hypixel.hytale.component)

## Core concepts
- `Component` / `ComponentType` — typed data attached to entities or stores.
- `ComponentRegistry` — central registry for component types and codecs.
- `Archetype` — immutable set of component types for fast querying.
- `Query` / `ExactArchetypeQuery` — component queries.
- `Store` / `Resource` — storage abstractions for ECS data.
- `SystemGroup` / `SystemType` — system grouping and type definitions.

## Subpackages
- **data** — component data types.
- **dependency** — component dependencies.
- **event** — component‑related events.
- **metric** — metrics for ECS.
- **query** — query builders and query types.
- **spatial** — spatial indexing.
- **system** — system execution utilities.
- **task** — ECS task helpers.

## Plugin‑side access
`PluginBase` exposes:
- `ComponentRegistryProxy<EntityStore>`
- `ComponentRegistryProxy<ChunkStore>`

These allow plugins to register or query ECS components within world/entity stores.
