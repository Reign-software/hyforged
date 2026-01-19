# Block Components (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/block-components

## Purpose
Create ticking block behavior using ECS components on the `ChunkStore`.

## Key concepts
- Define a block component that implements `Component<ChunkStore>` and provides a `BuilderCodec`.
- Implement a ticking system that iterates block sections and executes block logic.
- Convert local chunk coordinates into world coordinates before running logic.

## Plugin integration
- Register the block component with `getChunkStoreRegistry().registerComponent(...)`.
- Register the ticking system with `getChunkStoreRegistry().registerSystem(...)`.

## Notes
- The guide points to built‑in farming tick systems as a more complete reference.
