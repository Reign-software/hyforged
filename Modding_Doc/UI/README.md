# UI Modding (Custom .ui)

This guide documents the custom `.ui` system used by Hyforged and Hytale plugins.

## Overview

Hytale uses `.ui` files to define UI layouts. The server sends UI definitions to clients to render.

Two main UI paradigms:
- **HUDs** — Always-visible overlays (health bars, indicators)
- **Pages** — Modal interfaces that capture player focus

> Note: NoesisGUI (XAML) exists in the base client, but **custom `.ui` is the current modding system**.

---

## Setup Checklist

- Place `.ui` files under `resources/Common/UI/Custom/`
- Set `"IncludesAssetPack": true` in `manifest.json`
- Enable **Diagnostic Mode** in Hytale settings for UI errors

---

## .ui Syntax Quickstart

```ui
Group #Root {
  LayoutMode: Top;
  Padding: (Full: 20);

  Label #Title {
    Text: "Hello";
    Style: (FontSize: 24, Alignment: Center, RenderBold: true);
  }

  TextField #SearchInput {
    Anchor: (Width: 200, Height: 30);
    Background: $Common.@InputBoxBackground;
    Style: $Common.@DefaultInputFieldStyle;
  }
}
```

### Key Syntax Rules

- **Elements:** `Type [#Id] { ... }` (e.g., `Group`, `Label`, `Button`, `TextField`)
- **IDs:** prefix with `#` for Java access
- **Variables:** `@Name = ...` (local), `$Common = "Common.ui"` (imports)
- **Comments:** `// comment`

---

## Value Objects (from server code)

These objects are validated by server codecs and safe to document as valid.

### Anchor
Keys: `Left`, `Right`, `Top`, `Bottom`, `Width`, `Height`, `MinWidth`, `MaxWidth`, `Full`, `Horizontal`, `Vertical`

### Area
Keys: `X`, `Y`, `Width`, `Height`

### PatchStyle
Keys: `TexturePath`, `Border`, `HorizonzalBorder` *(spelled this way)*, `VerticalBorder`, `Color`, `Area`

```ui
@Panel = PatchStyle(TexturePath: "UI/Textures/Decorations/Panel.png", Border: 6, Color: #ffffff);
```

### LocalizableString
Accepts either a string or a message object:

```json
{ "MessageId": "server.ui.someKey", "MessageParams": { "count": "5" } }
```

### DropdownEntryInfo
Fields: `Label` (LocalizableString), `Value` (string)

### ItemGridSlot
Fields: `ItemStack`, `Background`, `Overlay`, `Icon`, `IsItemIncompatible`, `Name`, `Description`,
`SkipItemQualityBackground`, `IsActivatable`, `IsItemUncraftable`

---

## Element Property Reference (Observed in Server Code)

These properties are actively set by server UI code and are safe to document.

### Common
- `Visible` (bool)
- `Style` (tuple or reference)
- `Background` (PatchStyle or texture path)
- `Width` (int)

### Labels
- `Text` (string / LocalizableString)
- `TextSpans` (Message)
- `TextColor` (style tuple)
- `Color` (string)

### Inputs (TextField / Slider / CheckBox / ColorPicker)
- `Value` (string / number / boolean)
- `PlaceholderText` (string / Message)
- `Entries` (DropdownEntryInfo[])

### Buttons / interactive
- `Disabled` (bool)
- `TooltipText` (string)
- `TooltipTextSpans` (Message)

### Images / icons / item UI
- `Source` (texture path)
- `AssetPath` (string)
- `ItemId` (string)
- `Quantity` (int)
- `Slots` (ItemGridSlot[])

---

## UI Assets (lib/UI)

`lib/UI` contains Noesis XAML and textures used by the base client. Custom `.ui` does **not** use XAML directly, but you can reference textures from `lib/UI/Textures/**` as **read-only** assets.

---

## Generating .ui Files

- **Manual authoring** in a text editor
- **Hytale UI Builder**: https://hytale.ellie.au/ (visual editor)
- **HyUI** (Java builder API) for programmatic UI generation

---

## Using UI in Java

### HUDs
Extend `CustomUIHud` and append a `.ui` document:

```java
public class MyHud extends CustomUIHud {
    public MyHud(@Nonnull PlayerRef playerRef) { super(playerRef); }

    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        uiCommandBuilder.append("MyHud.ui");
    }
}
```

### Pages
Use `CustomUIPage` (non-interactive) or `InteractiveCustomUIPage<T>` (interactive):

```java
eventBuilder.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#SearchInput",
    EventData.of("@SearchQuery", "#SearchInput.Value"),
    false
);
```

**Important:** Always call `sendUpdate()` (or close the page) after handling events.

---

## Threading Warning (Pages)

Opening pages must occur on the main/world thread. Use `AbstractAsyncCommand` and schedule UI opens via the world executor.

---

## Troubleshooting

- **Failed to apply Custom UI HUD commands** → `.ui` syntax error; use Diagnostic Mode.
- **Could not find document** → file path wrong or not under `resources/Common/UI/Custom/`.
- **Page stuck on “Loading…”** → forgot `sendUpdate()` after event handling.
