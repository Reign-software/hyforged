# UI Modding (Native .ui System)

This guide documents Hytale's native `.ui` system for creating custom UIs in Hyforged.

## Overview

Hytale uses `.ui` files to define UI layouts. The server sends UI definitions to clients to render.

Two main UI paradigms:
- **HUDs** — Always-visible overlays (health bars, buff indicators, notifications)
- **Pages** — Modal interfaces that capture player focus (menus, dialogs)

> Note: NoesisGUI (XAML) exists in the base client, but **custom `.ui` is the modding system**.

---

## Setup Checklist

1. Place `.ui` files under `resources/Common/UI/Custom/`
2. Set `"IncludesAssetPack": true` in `manifest.json`
3. Image files must use `@2x.png` suffix (e.g., `MyIcon@2x.png`)
4. Enable **Diagnostic Mode** in Hytale settings for UI error debugging

---

## .ui Syntax Quickstart

```ui
// Import Common.ui for shared styles
$Common = "../Common.ui";

// Define local variables
@PanelBackground = PatchStyle(Color: #1a1a2eE0, Border: 4);

Group #Root {
    Anchor: (Full: 0);
    LayoutMode: Top;
    Padding: (Full: 20);

    Label #Title {
        Text: "Hello World";
        Style: (FontSize: 24, Alignment: Center, RenderBold: true);
    }

    TextField #SearchInput {
        Anchor: (Width: 200, Height: 30);
        PlaceholderText: "Search...";
        Style: $Common.@DefaultInputFieldStyle;
    }
    
    Button #SubmitBtn {
        Text: "Submit";
        Anchor: (Width: 100, Height: 36);
    }
}
```

### Key Syntax Rules

| Syntax | Description |
|--------|-------------|
| `Type #Id { }` | Element with ID (e.g., `Group #Root`, `Label #Title`) |
| `#Id` | ID prefix for Java/event access |
| `@Name = value;` | Local variable definition |
| `$Common = "path.ui";` | Import external .ui file |
| `$Common.@StyleName` | Reference variable from imported file |
| `%localization.key` | Localization string reference |
| `// comment` | Single-line comment |
| `(Key: value, ...)` | Tuple syntax for complex values |

---

## Element Types

### Primitive Elements

| Element | Purpose |
|---------|---------|
| `Group` | Container/layout (like div) |
| `Label` | Text display |
| `Button` | Clickable button |
| `TextField` | Text input |
| `Slider` | Range input |
| `CheckBox` | Toggle switch |
| `ColorPicker` | Color selection |
| `DropdownBox` | Dropdown selection |
| `ProgressBar` | Progress indicator |
| `CircularProgressBar` | Circular progress |
| `Sprite` | Animated image |
| `ItemIcon` | Item icon display |
| `ItemSlot` | Full item slot |
| `ItemGrid` | Scrollable item grid |
| `TabNavigation` | Tab bar |
| `TimerLabel` | Timer display |

### Macro Elements (from Common.ui)

Pre-styled components: `PageOverlay`, `Container`, `TextButton`, `SecondaryTextButton`, `TertiaryTextButton`, `CancelTextButton`, `BackButton`, `NumberField`, `AssetImage`, `CheckBoxWithLabel`

---

## Property Reference

### Anchor
Position and size an element:

```ui
Anchor: (Left: 10, Top: 10, Width: 200, Height: 50);
Anchor: (Full: 0);           // Fill parent
Anchor: (Horizontal: 10);    // Left and Right margin
```

Keys: `Left`, `Right`, `Top`, `Bottom`, `Width`, `Height`, `MinWidth`, `MaxWidth`, `Full`, `Horizontal`, `Vertical`

### LayoutMode
How children are arranged:

| Value | Description |
|-------|-------------|
| `Top` | Stack from top |
| `TopScrolling` | Stack from top with scroll |
| `Bottom` / `BottomScrolling` | Stack from bottom |
| `Left` / `Right` | Horizontal stack |
| `Center` / `Middle` | Center alignment |
| `CenterMiddle` | Center both axes |
| `Full` | Fill available space |

### Style Tuple
Text and visual styling:

```ui
Style: (FontSize: 18, RenderBold: true, Alignment: Center, TextColor: #FFFFFF);
```

Properties: `FontSize`, `FontName`, `RenderBold`, `RenderItalics`, `RenderUppercase`, `TextColor`, `Alignment`, `HorizontalAlignment`, `VerticalAlignment`, `Wrap`, `LetterSpacing`, `OutlineColor`

### PatchStyle (Backgrounds)
Nine-slice scalable backgrounds:

```ui
Background: PatchStyle(Color: #1a1a1eE0);
Background: PatchStyle(TexturePath: "Panel.png", Border: 6);
```

Keys: `TexturePath`, `Border`, `HorizonzalBorder` *(typo in API)*, `VerticalBorder`, `Color`, `Area`

### Other Properties

| Property | Elements | Type |
|----------|----------|------|
| `Visible` | All | bool |
| `Padding` | Group | tuple |
| `ScrollbarStyle` | Group | reference |
| `Text` | Label, Button | string |
| `TextSpans` | Label | Message |
| `Value` | TextField, Slider, etc. | varies |
| `PlaceholderText` | TextField | string |
| `Disabled` | Button | bool |
| `Min`, `Max`, `Step` | Slider | int |
| `Slots` | ItemGrid | ItemGridSlot[] |
| `SlotsPerRow` | ItemGrid | int |
| `ShowScrollbar` | ItemGrid | bool |

---

## Value Objects (Java Codecs)

### ItemGridSlot
Fields: `ItemStack`, `Background`, `Overlay`, `Icon`, `IsItemIncompatible`, `Name`, `Description`, `SkipItemQualityBackground`, `IsActivatable`, `IsItemUncraftable`

### DropdownEntryInfo
Fields: `Label` (LocalizableString), `Value` (string)

### LocalizableString
Either a plain string or `{ "MessageId": "key", "MessageParams": {...} }`

### Area
Fields: `X`, `Y`, `Width`, `Height`

---

## Java API

### CustomUIHud - Persistent Overlays

```java
public class MyHud extends CustomUIHud {
    public MyHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(UICommandBuilder cmd) {
        cmd.append("MyHud.ui");
    }
}

// Usage (on world thread)
new MyHud(playerRef).show();
```

### InteractiveCustomUIPage<T> - Interactive Pages

```java
public class MyPage extends InteractiveCustomUIPage<MyPage.EventData> {
    public MyPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("Pages/MyPage.ui");
        
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
            EventData.of("Action", "save"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
            EventData.of("@Query", "#SearchInput.Value"), false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EventData data) {
        // Handle events...
        sendUpdate(null, false);  // IMPORTANT: Always call after handling
    }

    public static class EventData {
        public static final BuilderCodec<EventData> CODEC = BuilderCodec.builder(EventData.class, EventData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action).add()
            .append(new KeyedCodec<>("@Query", Codec.STRING), (e, s) -> e.query = s, e -> e.query).add()
            .build();
        String action;
        String query;
    }
}
```

### UICommandBuilder Methods

```java
cmd.append("Pages/MyPage.ui");                    // Append .ui file
cmd.append("#Container", "Pages/Item.ui");       // Append to selector
// IMPORTANT: Text values MUST be quoted in inline .ui syntax
cmd.appendInline("#List", "Label { Text: \"X\"; }");  // Inline UI
cmd.set("#Label.Text", "Hello");                 // Set property
cmd.set("#Label.Visible", true);
cmd.setObject("#Grid.Slots", slots);             // Set object array
cmd.set("#Btn.Style", Value.ref("Common.ui", "DefaultButtonStyle"));  // Reference
cmd.clear("#Container");                          // Clear children
cmd.remove("#Element");                           // Remove element
```

### Event Types (CustomUIEventBindingType)

| Event | Trigger |
|-------|---------|
| `Activating` | Button click |
| `ValueChanged` | Input value change |
| `FocusGained` / `FocusLost` | Focus changes |
| `MouseEntered` / `MouseExited` | Hover |
| `SlotClicking` | ItemGrid slot click |
| `SelectedTabChanged` | Tab switch |

---

## Threading (CRITICAL)

**UI operations MUST run on the world thread** or the client will disconnect.

```java
World world = store.getExternalData().getWorld();
world.execute(() -> {
    // UI operations here
    new MyHud(playerRef).show();
});
```

---

## Scrolling Content

Use `LayoutMode: TopScrolling` for scrollable groups:

```ui
Group #ScrollContainer {
    Anchor: (Full: 0);
    LayoutMode: TopScrolling;
    ScrollbarStyle: $Common.@DefaultScrollbarStyle;
    
    // Children scroll vertically
}
```

---

## Assets

- Put textures in `resources/Common/UI/Custom/`
- Files must use `@2x.png` suffix (e.g., `Icon@2x.png`)
- Reference as `TexturePath: "Icon.png"` (without @2x)

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Failed to apply Custom UI HUD commands` | .ui syntax error; enable Diagnostic Mode |
| `Could not find document` | Wrong path or not in `Common/UI/Custom/` |
| Page stuck on "Loading…" | Forgot `sendUpdate()` after event handling |
| Client disconnect | Not running on world thread |
| Texture not showing | Missing `@2x.png` suffix |

---

## Tools

- **Hytale UI Builder**: https://hytale.ellie.au/ (visual editor)
- **Manual authoring**: Any text editor

---

## See Also

- [Hytale UI Modding Skill](/.github/skills/hytale-ui-modding/SKILL.md) - Full reference
- [Hytale Modding Docs](https://hytalemodding.dev/en/docs/guides/plugin/ui)
