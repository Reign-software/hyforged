---
name: hytale-ui-modding
description: Guidance for Hytale plugin UI modding with .ui files, CustomUIHud, CustomUIPage, and InteractiveCustomUIPage. Use when creating or updating custom HUDs/pages, binding UI events, or troubleshooting UI asset paths.
---

# Hytale UI Modding Skill

Use this skill when working on Hytale plugin UI defined in .ui files and rendered via Custom UI HUDs or Pages.

## Key Requirements

- Place all .ui files under resources/Common/UI/Custom.
- Ensure manifest.json includes "IncludesAssetPack": true.
- Use Hytale client Diagnostic Mode for detailed UI error messages.

## .ui File Basics

- UI is defined in .ui files (tree of UI elements; currently the only UI system in Hytale).
- Elements are declared by type and can be nested:
  - Examples: Group, Label, TextField.
- Use element IDs (e.g., #MyInput) when Java needs to access the element.
- Variables use @-prefix.
- Textures can be used via PatchStyle(TexturePath: "MyBackground.png").
- Include other .ui files via a variable, e.g. $Common = "Common.ui"; then access its variables with $Common.@VarName.

## .ui Format & Schema Details

- General element declaration format:
  - Type [#Id] { Property: Value; Property: Value; }
  - Example properties used in docs: Style, Background, Anchor, LayoutMode, Padding, Text.
- IDs:
  - Prefix with # to reference in Java (e.g., #MyInput, #MyLabel).
- Variables:
  - Use @ for local variables (e.g., @MyTex = PatchStyle(...)).
  - Use $ for included documents (e.g., $Common = "Common.ui").
- Textures:
  - PatchStyle(TexturePath: "MyBackground.png") loads textures relative to the .ui file.
- Anchors and layout:
  - Anchor tuples commonly include Width/Height and positional values (e.g., Top).
  - LayoutMode values are used to control child layout (e.g., Center, Top in examples).

## UI Command Schema (HUD/Page Build)

- UICommandBuilder.append("MyUI.ui") adds a document from resources/Common/UI/Custom.
- UICommandBuilder.set("#Element.Property", value) updates UI at runtime.
- update(false, builder) updates without clearing existing UI; true clears existing UI.

## Interactive Page Data Schema

- InteractiveCustomUIPage<Data> requires a Data class that maps UI values to server fields.
- Data codec pattern (conceptual):
  - BuilderCodec.builder(Data.class, Data::new)
  - .append(new KeyedCodec<>("@Key", Codec.STRING), setter, getter)
  - .add().build()
- Event binding schema:
  - UIEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Element",
    EventData.of("@Key", "#Element.Value"), false)
- The key in EventData.of("@Key", ...) must match the codec key in Data.
- After handling Data events, always call sendUpdate() or switch pages.

## Deprecation Note

- .ui files are currently the only supported UI system; NoesisGUI is planned but not available yet.

## HUDs (Always-on UI)

### CustomUIHud

- Extend CustomUIHud.
- In build(), use UICommandBuilder.append("MyUI.ui") to load UI.
- The .ui file path is relative to resources/Common/UI/Custom.

### Showing & Hiding

- Use Player#getHudManager().
- Show: HudManager#setCustomHud(...).
- Hide default UI: HudManager#hideHudComponents(...).
- Hide custom UI by setting an empty HUD or using the MultipleHUD approach.

## UI Pages (Modal UI)

Pages block game input and unlock the mouse.

### CustomUIPage (No input)

- Extend CustomUIPage for non-interactive UI pages.
- Similar to CustomUIHud usage with UICommandBuilder.append().

### InteractiveCustomUIPage (Input + events)

- Extend InteractiveCustomUIPage<Data> to receive input events.
- Define a Data class with BuilderCodec and KeyedCodec mappings.
- Bind events in build() using UIEventBuilder.addEventBinding().
- Always call sendUpdate() or swap pages after handling events to avoid a stuck “Loading...” state.

## Event Binding Pattern

- Bind a UI event (e.g., ValueChanged on #MyInput) to a data key (e.g., @MyInput).
- Use EventData.of("@MyInput", "#MyInput.Value") to map client values to your Data codec key.

## Dynamic Updates

- Use UICommandBuilder.set("#MyLabel.TextSpans", Message.raw(newText)) and update(false, builder) to update parts of UI without reloading everything.

## Common Issues

- Failed to apply Custom UI HUD commands: .ui file syntax error; check Diagnostic Mode for details.
- Could not find document for Custom UI Append: .ui file path is wrong or file not under resources/Common/UI/Custom.

## Recommended Checklist

1. Put .ui and texture assets in the correct resource folders.
2. Add "IncludesAssetPack": true to manifest.json.
3. Verify element IDs in .ui match Java bindings.
4. Ensure event bindings map to codec keys.
5. Call sendUpdate() after handling InteractiveCustomUIPage events.
6. Validate paths and enable Diagnostic Mode when errors occur.

## References

- https://hytalemodding.dev/en/docs/guides/plugin/ui
- https://github.com/underscore95/Hytale-Sandbox-Plugin/tree/ui-pages/src/main/resources/Common/UI/Custom
