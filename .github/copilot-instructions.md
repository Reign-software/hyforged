# Hytale plugin
This is a Hytale plugin project. Hytale plugins are used to extend the functionality of the Hytale server. Plugins can add new features, modify existing behavior, or integrate with other systems. Plugins are typically written in Java and packaged as JAR files that can be loaded by the Hytale server. The plugins are sent to the client at runtime so they are only needed on the server.

- The `.memory_bank` directory contains important context such as ADRs, requirements, and design decisions. Keep this up to date as you work on the project.
- Hytale uses an Entity Component System (ECS) architecture. Very data driven. Do not hard code values.
- Before implementing new features, review the ECS patterns and existing components in the Hytale server code.
- Use `.doc` for looking up domain knowledge.
- The source code for the Hytale server can be found in the `lib/hytale-server/src/main/java/com/hypixel` directory.
- The games JSON that makes up all items, blocks, and other in-game assets can be found in the `lib/Server` directory. you can use this to look up item IDs, block IDs, and other in-game assets. Do not modify these files directly, they are for reference. We have our own data under `src/main/resources/Server/Hyforged`.
- The `Modding_Doc` folder can be used to store documentation related to modding Hyforged as well as references. Ensure this stays up to date with relevant user-facing documentation about modding Hyforged.
- Avoid enums, this is data driven from JSON files via resources. Reference `lib/Server` directory for structure and json examples.
- Review TODOs when implementing a plan as they may be from a previous implementation or design decision awaiting the plan.
- When working with systems, aim to make things generic and data driven. Leverage tags and JSON data wherever possible.
- DO NOT hard code values; always use data-driven approaches and JSON configuration.
- Prefer single-file JSON definitions for features that extend Hytale (e.g., buffs/debuffs, effects, interactions). Avoid multi-file JSON solutions unless there is a clear, logical design need.

## .github/skills
- Evaluate skills when given a task or problem to solve.
- Multiple skills may be required to complete a task or solve a problem effectively.
- Skills should be applied in a context-aware manner, considering the specific requirements and constraints of the task.
- Continuously evaluate and update the skill set as new information and context become available.

## Hytale ECS notes (follow these patterns)
- ECS is composition over inheritance. Entities are identifiers only, Components are pure data, Systems contain logic.
- Use `Store<EntityStore>` to access component data. Do not keep direct references to entity objects; use `Ref<EntityStore>` handles and validate as needed.
- `Store` uses archetypes (chunked storage). Keep systems query-driven and data-oriented.
- `EntityStore` provides world access and entity lookup (UUID, network id). `ChunkStore` is for chunk/block data and world chunks.
- Build entities via `Holder<EntityStore>` then add to the store; treat it as a staging cart for components.
- Components implement `Component<EntityStore>` and must provide a default constructor and `clone()` (copy constructor pattern).
- Use `CommandBuffer` for entity/component changes instead of mutating the store directly (thread safety + ordering).
- Systems:
	- `EntityTickingSystem` for per-entity tick logic.
	- `TickingSystem` for global per-tick logic.
	- `DelayedEntitySystem` for interval-based entity updates.
	- `RefChangeSystem` for reacting to component add/set/remove.
- Queries filter entities (`Query.and`, `Query.not`). Only entities matching the query are processed.
- Use `SystemGroup` and dependencies to control execution order (e.g., damage pipeline stages).
- Register components and systems in plugin `setup()` via `EntityStoreRegistry.registerComponent` and `registerSystem`. Keep `ComponentType` references for reuse.
- Use ECS access patterns shown in official docs:
	- https://hytalemodding.dev/en/docs/guides/ecs/entity-component-system
	- https://hytalemodding.dev/en/docs/guides/ecs/hytale-ecs-theory
	- https://hytalemodding.dev/en/docs/guides/ecs/systems
	- https://hytalemodding.dev/en/docs/guides/ecs/example-ecs-plugin

## Project Structure
```text
your-plugin-name/
|-- src/
|   `-- main/
|       |-- java/
|       |   `-- com/
|       |       `-- yourname/
|       |           `-- yourplugin/
|       |               `-- YourPlugin.java
|       `-- resources/
|           |-- manifest.json
|           |-- Common/          # Assets (models, textures)
|           `-- Server/          # Server-side data
|-- build.gradle
|-- settings.gradle
|-- gradle.properties
|-- README.md
`-- run/                         # Generated when you run the server

```