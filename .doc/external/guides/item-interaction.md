# Custom Item and Interaction (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/item-interaction

## Purpose
Create a custom item with a custom interaction behavior.

## Key concepts
- Enable asset packs in `manifest.json` (`IncludesAssetPack`).
- Define the item in `Server/Item/Items/...` with icon, model, texture, ID, categories, and stack size.
- Add a crafting recipe in the item definition (optional).
- Implement an interaction by extending `SimpleInstantInteraction` and providing a `BuilderCodec`.
- Register the interaction with `getCodecRegistry(Interaction.CODEC).register("id", class, codec)`.
- Link the interaction by adding an `Interactions` block in the item JSON.

## Notes
- Interactions often use `CommandBuffer` and store access for thread‑safe changes.
