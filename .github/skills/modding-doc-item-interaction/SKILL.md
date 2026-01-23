---
name: modding-doc-item-interaction
description: Create custom items and item interactions in Hytale plugins, including item JSON, recipes, interaction classes, codec registration, and chained interaction JSON. Triggers - item interaction, custom item, item json, recipe, SimpleInstantInteraction, interaction codec, charging, condition, serial, replace.
---

# Hytale Plugin: Item Interaction Skill

Use this skill when you need to create a custom item and wire a custom interaction to it.

## When to use
- Adding a new custom item with its own model/texture/icon.
- Defining interaction behavior in Java (e.g., use/right-click actions).
- Linking JSON interaction chains to items.
- Registering custom interaction codecs in a plugin.

## Setup checklist (assets + data)
1. Ensure `manifest.json` enables asset packs (`IncludesAssetPack: true`).
2. Create server-side item data:
   - `Server/Item/Items/<item_id>.json` (item definition).
3. Create client assets:
   - `Common/Icons/ItemsGenerated/<icon>.png`
   - `Common/Items/<item_folder>/model.blockymodel`
   - `Common/Items/<item_folder>/model_texture.png`

## Item definition essentials (Server/Item/Items)
Include at minimum:
- `TranslationProperties` (Name/Description or localization keys)
- `Id` (unique item id)
- `Icon` (path to icon)
- `Model` and `Texture`
- `Quality`, `MaxStack`, `Categories`

Optional: add a `Recipe` block for crafting. Keep values data-driven and avoid hard-coded assumptions.

## Adding a recipe (optional)
Add a `Recipe` object in the item JSON to specify:
- `TimeSeconds`
- `Input` list of `{ ItemId, Quantity }`
- `BenchRequirement` with `Id`, `Type`, and `Categories`

## Custom interaction (Java)
Create a class that extends `SimpleInstantInteraction` and overrides `firstRun(...)`.

### Required: Codec
Define a `BuilderCodec` so the interaction can be referenced by JSON:
- Use `BuilderCodec.builder(MyInteraction.class, MyInteraction::new, SimpleInstantInteraction.CODEC).build()`

### Register interaction
In your plugin `setup()`:
- Register the interaction codec with `getCodecRegistry(Interaction.CODEC)`
- Use a unique interaction id (string key)

## Link interaction to item JSON
Add an `Interactions` block to the item JSON. Example shape:
- `Interactions.Secondary.Interactions[]` contains your custom interaction by `Type` id.
- You can also use `Primary` depending on the action you want to override.

## Advanced interaction chains (JSON)
Interactions are nested and can be composed to create complex behavior:
- `Condition`: gate on states like crouching.
- `Charging`: require a hold/charge time (seconds as key).
- `Serial`: run multiple interactions in order.
- `Replace`: override default item interaction.
- `Simple`: basic interaction wrapper.

You can chain these blocks to build flows such as: Condition → Charging → Custom Interaction, with `Failed` fallbacks.

## Implementation notes
- Favor data-driven JSON composition over hard-coded behavior.
- Keep interaction IDs stable; they are referenced by item data.
- Validate references (icon/model/texture paths) to avoid missing-asset issues.
- Use logging and `InteractionState` to report failures in custom interactions.
- Full information can be found here: https://hytalemodding.dev/en/docs/guides/plugin/item-interaction

## Minimal workflow summary
1. Add item JSON + assets.
2. Implement interaction class + codec.
3. Register codec in plugin `setup()`.
4. Reference interaction ID in item JSON `Interactions`.
5. Test the interaction in-game and iterate with chained interactions if needed.
