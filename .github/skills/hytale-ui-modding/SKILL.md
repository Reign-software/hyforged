---
name: hytale-ui-modding
description: Comprehensive guidance for Hytale plugin UI modding using native .ui files, CustomUIHud, MHUD (multiple HUDs), CustomUIPage, and InteractiveCustomUIPage. Use when creating or updating custom HUDs/pages, writing .ui markup, binding UI events, using MultipleHUD library, or troubleshooting UI issues.
---

# Hytale Native UI Modding Skill

Use this skill when working on Hytale plugin UI using the **native .ui file system** and Java UI classes. This covers `.ui` file syntax, element types, properties, and the server-side Java API.

> **Important**: This project uses native Hytale UI only. Do not use HyUI library.

---

## Quick Reference

| Concept | Description |
|---------|-------------|
| **.ui files** | Hytale's native UI markup language (NOT HTML/CSS) |
| **CustomUIHud** | Always-visible overlay elements (Never use single hud, instead use MultipleHUD) |
| **MHUD / MultipleHUD** | Library enabling multiple simultaneous HUDs per player |
| **CustomUIPage** | Static full-screen modal pages |
| **InteractiveCustomUIPage<T>** | Interactive pages with event handling |
| **UICommandBuilder** | Java API for building/modifying UI |
| **UIEventBuilder** | Java API for binding events |

---

## .ui File Syntax

### File Location
```
src/main/resources/Common/UI/Custom/
```

### Basic Structure

```ui
// Comments use double slashes
$Common = "../Common.ui";  // Import another .ui file

@MyVariable = "some value";  // Local variable
@MyStyle = (FontSize: 18, RenderBold: true);  // Style tuple

Group #Root {
    Anchor: (Full: 0);
    LayoutMode: Top;
    Background: PatchStyle(Color: #1a1a1eE0);
    
    Label #Title {
        Text: "Hello World";
        Style: (FontSize: 24, RenderBold: true, Alignment: Center);
    }
    
    Button #MyButton {
        Text: "Click Me";
        Anchor: (Width: 200, Height: 40);
    }
}
```

### Syntax Rules

- **Elements**: `ElementType [#Id] { properties... }`
- **IDs**: Prefix with `#` for Java/event access
- **Variables**: `@Name = value;` (local) or `$Name = "path.ui";` (import)
- **Properties**: `PropertyName: value;`
- **Tuples**: `(Key1: value1, Key2: value2)`
- **References**: `$Common.@StyleName` references a style from imported file
- **Localization**: `%localization.key` for translatable strings
- **Comments**: `// single line comment`

---

## Element Types

### Primitive Elements

| Element | Purpose | Key Properties |
|---------|---------|----------------|
| `Group` | Container/layout | `LayoutMode`, `Background`, `Padding`, `ScrollbarStyle` |
| `Label` | Text display | `Text`, `TextSpans`, `Style`, `TextColor` |
| `Button` | Clickable button | `Text`, `Disabled`, `Style`, `Background` |
| `TextField` | Text input | `Value`, `PlaceholderText`, `MaxLength`, `ReadOnly`, `Password`, `PasswordChar`, `AutoGrow`, `MaxVisibleLines` |
| `Slider` | Range input | `Value`, `Min`, `Max`, `Step`, `Style` |
| `CheckBox` | Toggle input | `Value` (boolean) |
| `ColorPicker` | Color selector | `Value` |
| `DropdownBox` | Dropdown selection | `Value`, `Entries` (DropdownEntryInfo[]) |
| `ProgressBar` | Progress display | `Value` (0.0-1.0), `BarTexturePath`, `EffectTexturePath`, `Direction`, `Alignment`, `Color` |
| `CircularProgressBar` | Circular progress | `Value`, `MaskTexturePath` |
| `Sprite` | Animated image | `TexturePath`, `Frame`, `FramesPerSecond` |
| `ItemIcon` | Item display | `ItemId`, `Quantity` |
| `ItemSlot` | Full item slot | `ItemStack`, `Background`, `Overlay`, `Icon` |
| `ItemGrid` | Scrollable item grid | `Slots`, `SlotsPerRow`, `AreItemsDraggable`, `ShowScrollbar`, `KeepScrollPosition`, `RenderItemQualityBackground` |
| `TabNavigation` | Tab bar | (use with tab content groups) |
| `TimerLabel` | Timer display | (specialized label) |

### Macro Elements (from Common.ui)

These are pre-styled components defined in Common.ui:

| Macro | Purpose |
|-------|---------|
| `PageOverlay` | Full-screen overlay background |
| `Container` | Styled window frame with title |
| `TextButton` | Primary styled button |
| `SecondaryTextButton` | Secondary styled button |
| `TertiaryTextButton` | Tertiary styled button |
| `CancelTextButton` | Cancel/destructive button |
| `BackButton` | Back navigation button |
| `NumberField` | Numeric input field |
| `AssetImage` | Asset image display |
| `CheckBoxWithLabel` | Checkbox with text label |

---

## Property Reference

### Anchor Properties

Controls element positioning and sizing:

```ui
Anchor: (Left: 10, Top: 10, Width: 200, Height: 50);
Anchor: (Full: 0);           // Fill parent with 0px margin
Anchor: (Horizontal: 10);    // Left and Right = 10
Anchor: (Vertical: 5);       // Top and Bottom = 5
```

| Property | Type | Description |
|----------|------|-------------|
| `Left` | int | Distance from left edge |
| `Right` | int | Distance from right edge |
| `Top` | int | Distance from top edge |
| `Bottom` | int | Distance from bottom edge |
| `Width` | int | Fixed width |
| `Height` | int | Fixed height |
| `MinWidth` | int | Minimum width |
| `MaxWidth` | int | Maximum width |
| `Full` | int | Shorthand for all sides (margin) |
| `Horizontal` | int | Shorthand for Left + Right |
| `Vertical` | int | Shorthand for Top + Bottom |

### LayoutMode Values

Controls how child elements are arranged:

| Value | Description |
|-------|-------------|
| `Top` | Stack children from top |
| `TopScrolling` | Stack from top with vertical scrolling |
| `Bottom` | Stack children from bottom |
| `BottomScrolling` | Stack from bottom with vertical scrolling |
| `Left` | Stack children from left |
| `Right` | Stack children from right |
| `Center` | Center children horizontally |
| `Middle` | Center children vertically |
| `CenterMiddle` / `MiddleCenter` | Center both axes |
| `Full` | Fill available space |
| `LeftCenterWrap` | Left-aligned with center wrapping |
| `RightCenterWrap` | Right-aligned with center wrapping |

### Style Properties

Style tuples control text and visual appearance:

```ui
Style: (FontSize: 18, RenderBold: true, Alignment: Center);
```

| Property | Values/Type | Description |
|----------|-------------|-------------|
| `FontSize` | int | Text size (e.g., 14, 18, 24) |
| `FontName` | string | Font family |
| `RenderBold` | bool | Bold text |
| `RenderItalics` | bool | Italic text |
| `RenderUppercase` | bool | Uppercase transform |
| `TextColor` | hex | Text color (#RRGGBB or #RRGGBBAA) |
| `Alignment` | enum | Label alignment: `Center` only. Do NOT use `Left` or `Right` - text is left-aligned by default. Use container `LayoutMode` for positioning. |
| `HorizontalAlignment` | enum | `Left`, `Right`, `Center` |
| `VerticalAlignment` | enum | `Top`, `Bottom`, `Center`, `Middle` |
| `Wrap` | bool | Text wrapping |
| `LetterSpacing` | int | Character spacing |
| `OutlineColor` | hex | Text outline color |

### PatchStyle (Backgrounds)

Nine-slice scalable backgrounds:

```ui
Background: PatchStyle(Color: #1a1a1eE0);
Background: PatchStyle(TexturePath: "UI/Textures/Panel.png", Border: 6);
Background: PatchStyle(TexturePath: "MyTexture.png", HorizonzalBorder: 4, VerticalBorder: 8);
```

| Property | Type | Description |
|----------|------|-------------|
| `TexturePath` | string | Path to texture file |
| `Border` | int | Border slice size (all sides) |
| `HorizonzalBorder` | int | Horizontal border (note: typo in Hytale API) |
| `VerticalBorder` | int | Vertical border |
| `Color` | hex | Background color |
| `Area` | Area | Texture source area |

### Area Object

```ui
Area: (X: 0, Y: 0, Width: 64, Height: 64);
```

### Padding

```ui
Padding: (Full: 20);
Padding: (Left: 10, Top: 20, Right: 10, Bottom: 20);
```

---

## Value References

Reference values from other documents:

```ui
// In your .ui file
$Common = "../Common.ui";
Style: $Common.@DefaultLabelStyle;
ScrollbarStyle: $Common.@DefaultScrollbarStyle;
```

In Java:
```java
import com.hypixel.hytale.server.core.ui.Value;

// Reference a style from Common.ui
commands.set("#Element.Style", Value.ref("Common.ui", "DefaultButtonStyle"));
```

---

## Java UI Classes

### CustomUIHud - Persistent Overlays

```java
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class MyHud extends CustomUIHud {
    public MyHud(PlayerRef playerRef) {
        super(playerRef);
    }

    @Override
    protected void build(UICommandBuilder commandBuilder) {
        commandBuilder.append("MyHud.ui");
        // Or inline:
        commandBuilder.appendInline(null, "Label #Status { Text: \"Hello\"; }");
    }
}

// Usage
MyHud hud = new MyHud(playerRef);
hud.show();
```

### Multiple HUDs with MHUD Library

By default, Hytale only allows **one** `CustomUIHud` per player. The **MHUD** library (by Buuz135) provides a wrapper that allows multiple HUD elements simultaneously.

**Dependency**: Already included in project via CurseForge Maven (`com.buuz135:MultipleHUD:1.0.2`)

```java
import com.buuz135.mhud.MultipleHUD;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

// Add/replace a HUD with a unique identifier
MultipleHUD.getInstance().setCustomHud(player, playerRef, "MyHudId", new MyCustomHud(playerRef));

// Add multiple HUDs
MultipleHUD.getInstance().setCustomHud(player, playerRef, "HealthBar", new HealthBarHud(playerRef));
MultipleHUD.getInstance().setCustomHud(player, playerRef, "Buffs", new BuffDisplayHud(playerRef));
MultipleHUD.getInstance().setCustomHud(player, playerRef, "Minimap", new MinimapHud(playerRef));

// Remove a specific HUD by identifier
MultipleHUD.getInstance().hideCustomHud(player, playerRef, "MyHudId");

// Replace a HUD (same identifier replaces existing)
MultipleHUD.getInstance().setCustomHud(player, playerRef, "HealthBar", new NewHealthBarHud(playerRef));
```

#### MHUD API Reference

| Method | Description |
|--------|-------------|
| `MultipleHUD.getInstance()` | Get the singleton instance |
| `setCustomHud(player, playerRef, id, hud)` | Add or replace a HUD by identifier |
| `hideCustomHud(player, playerRef, id)` | Remove a HUD by identifier |

#### How It Works

MHUD creates a wrapper `MultipleCustomUIHud` that contains a root group `#MultipleHUD`. Each individual HUD is added as a child group with ID `#<normalizedId>`. The library automatically:
- Converts HUD identifiers to valid element IDs (strips non-alphanumeric chars)
- Prefixes all selectors in your HUD with the container path
- Handles build/update lifecycle for each HUD independently

#### Empty HUD Placeholder

Use `EmptyHUD` as a placeholder when you need a HUD slot but no content:

```java
import com.buuz135.mhud.EmptyHUD;

// Create an empty placeholder
MultipleHUD.getInstance().setCustomHud(player, playerRef, "Placeholder", new EmptyHUD(playerRef));
```

#### Recommended ECS Pattern for HUD Systems

When creating HUD systems for Hyforged, follow this pattern (used by `CurrencyHudSystem`, `ResourceStatsHudSystem`, `CombatLogHudSystem`):

```java
public class MyHudSystem extends DelayedEntitySystem<EntityStore> {
    
    /** Check for MHUD availability at class load */
    private static final boolean MULTIPLE_HUD_AVAILABLE;
    static {
        boolean available = false;
        try {
            Class.forName("com.buuz135.mhud.MultipleHUD");
            available = true;
        } catch (ClassNotFoundException e) {
            LOGGER.warning("MultipleHUD not available - HUD disabled");
        }
        MULTIPLE_HUD_AVAILABLE = available;
    }
    
    /** Unique namespaced ID for this HUD */
    public static final String HUD_ID = "hyforged:my_hud";
    
    /** Track HUD instances per player */
    private static final Map<UUID, MyHud> playerHuds = new ConcurrentHashMap<>();
    
    @Override
    public void tick(...) {
        if (!MULTIPLE_HUD_AVAILABLE) return;
        
        UUID playerUuid = uuidComponent.getUuid();
        boolean shouldShowHud = /* your logic */;
        
        com.buuz135.mhud.MultipleHUD multipleHUD = com.buuz135.mhud.MultipleHUD.getInstance();
        MyHud existingHud = playerHuds.get(playerUuid);
        
        if (!shouldShowHud) {
            if (existingHud != null) {
                multipleHUD.hideCustomHud(player, playerRef, HUD_ID);
                playerHuds.remove(playerUuid);
            }
            return;
        }
        
        // Create HUD if not exists
        if (existingHud == null) {
            MyHud hud = new MyHud(playerRef);
            multipleHUD.setCustomHud(player, playerRef, HUD_ID, hud);
            playerHuds.put(playerUuid, hud);
            existingHud = hud;
        }
        
        // Update HUD with new values
        existingHud.updateValues(...);
    }
}
```

**Key points:**
- Use `DelayedEntitySystem` to avoid updating every tick
- Check `MULTIPLE_HUD_AVAILABLE` before any MHUD calls
- Use namespaced HUD IDs like `"hyforged:my_hud"`
- Track HUD instances per player UUID
- Hide HUD before removing from tracking map
- Only create new HUD if one doesn't exist for the player

---

### CustomUIPage - Static Pages

```java
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;

public class MyPage extends CustomUIPage {
    public MyPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commandBuilder, 
                      UIEventBuilder eventBuilder, Store<EntityStore> store) {
        commandBuilder.append("Pages/MyPage.ui");
    }
}
```

### InteractiveCustomUIPage<T> - Interactive Pages

```java
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;

public class MyInteractivePage extends InteractiveCustomUIPage<MyInteractivePage.EventData> {

    public MyInteractivePage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commandBuilder,
                      UIEventBuilder eventBuilder, Store<EntityStore> store) {
        commandBuilder.append("Pages/MyPage.ui");
        
        // Bind button click event
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#MyButton",
            EventData.of("Action", "buttonClicked"),
            false  // locksInterface
        );
        
        // Bind text input value change
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of("@SearchValue", "#SearchInput.Value"),
            false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EventData data) {
        if ("buttonClicked".equals(data.action)) {
            // Handle button click
        }
        if (data.searchValue != null) {
            // Handle search input change
            updateSearchResults(data.searchValue);
        }
        // IMPORTANT: Always call sendUpdate after handling events
        sendUpdate(null, false);
    }
    
    private void updateSearchResults(String query) {
        UICommandBuilder commands = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        // Build updated content...
        sendUpdate(commands, events, false);
    }

    // Event data class with codec
    public static class EventData {
        public static final BuilderCodec<EventData> CODEC = BuilderCodec.builder(EventData.class, EventData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (e, s) -> e.action = s, e -> e.action)
            .add()
            .append(new KeyedCodec<>("@SearchValue", Codec.STRING), (e, s) -> e.searchValue = s, e -> e.searchValue)
            .add()
            .build();

        String action;
        String searchValue;
    }
}
```

---

## UICommandBuilder Methods

```java
UICommandBuilder commands = new UICommandBuilder();

// Append .ui file content
commands.append("Pages/MyPage.ui");              // Append to root
commands.append("#Container", "Pages/Item.ui"); // Append to selector

// Append inline UI content
// IMPORTANT: Text values MUST be quoted in inline .ui syntax
commands.appendInline("#List", "Label { Text: \"Item\"; }");

// Insert before element
commands.insertBefore("#Target", "Pages/Header.ui");
commands.insertBeforeInline("#Target", "Label { Text: \"Before\"; }");

// Set properties
commands.set("#Label.Text", "Hello World");
commands.set("#Label.Visible", true);
commands.set("#Slider.Value", 50);
commands.set("#Progress.Value", 0.75f);

// Set complex objects
commands.setObject("#Element.Anchor", new Anchor().setWidth(Value.of(200)));
commands.setObject("#Grid.Slots", new ItemGridSlot[]{ new ItemGridSlot(itemStack) });

// Set with value reference
commands.set("#Button.Style", Value.ref("Common.ui", "DefaultButtonStyle"));

// Remove/clear
commands.remove("#Element");     // Remove element
commands.clear("#Container");    // Clear children
commands.setNull("#Label.Text"); // Set to null
```

---

## UIEventBuilder & Event Types

```java
UIEventBuilder events = new UIEventBuilder();

// Basic event binding
events.addEventBinding(CustomUIEventBindingType.Activating, "#Button");

// With data payload
events.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#Button",
    EventData.of("Action", "save").append("ItemId", itemId),
    false  // locksInterface - if true, locks UI during processing
);

// Value reference (gets value from UI element)
events.addEventBinding(
    CustomUIEventBindingType.ValueChanged,
    "#TextField",
    EventData.of("@Value", "#TextField.Value"),  // @ prefix = UI value reference
    false
);
```

### Event Types (CustomUIEventBindingType)

| Event | Trigger |
|-------|---------|
| `Activating` | Button click, element activation |
| `RightClicking` | Right mouse click |
| `DoubleClicking` | Double click |
| `MouseEntered` | Mouse enters element |
| `MouseExited` | Mouse leaves element |
| `ValueChanged` | Input value changes (TextField, Slider, etc.) |
| `FocusGained` | Element gains focus |
| `FocusLost` | Element loses focus |
| `KeyDown` | Key pressed while focused |
| `Validating` | Input validation |
| `Dismissing` | Page dismiss attempt |
| `SelectedTabChanged` | Tab selection changes |
| `SlotClicking` | ItemGrid slot clicked |
| `SlotDoubleClicking` | ItemGrid slot double-clicked |
| `SlotMouseEntered` | Mouse enters slot |
| `SlotMouseExited` | Mouse leaves slot |
| `DragCancelled` | Drag operation cancelled |
| `Dropped` | Item dropped |
| `SlotMouseDragCompleted` | Drag completed over slot |
| `SlotMouseDragExited` | Drag exited slot |
| `SlotClickReleaseWhileDragging` | Click released while dragging |
| `SlotClickPressWhileDragging` | Click pressed while dragging |
| `ElementReordered` | Element order changed |
| `MouseButtonReleased` | Mouse button released |

---

## Page Lifetime Options

```java
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;

CustomPageLifetime.CanDismiss   // Player can close with ESC
CustomPageLifetime.Dismiss      // Closes immediately (not typically used)
```

---

## Value Objects Reference

### ItemGridSlot

```java
new ItemGridSlot()
    .setItemStack(new ItemStack(itemId, quantity))
    .setBackground(Value.of(patchStyle))
    .setOverlay(Value.of(overlayStyle))
    .setIcon(Value.of(iconStyle))
    .setName("Custom Name")
    .setDescription("Custom description")
    .setItemIncompatible(false)
    .setActivatable(true)
    .setItemUncraftable(false);
```

### DropdownEntryInfo

```java
new DropdownEntryInfo(LocalizableString.fromString("Option 1"), "value1")
```

### LocalizableString

```java
// Plain string
LocalizableString.fromString("Hello World")

// Localization key
LocalizableString.fromMessageId("server.ui.myKey")

// With parameters
LocalizableString.fromMessageId("server.ui.greeting", Map.of("name", playerName))
```

---

## Scrolling Groups

For scrollable content, use `LayoutMode: TopScrolling` (or `BottomScrolling`):

```ui
Group #ScrollContainer {
    Anchor: (Full: 10);
    LayoutMode: TopScrolling;
    ScrollbarStyle: $Common.@DefaultScrollbarStyle;
    
    // Children will scroll
    Group #Item1 { ... }
    Group #Item2 { ... }
    Group #Item3 { ... }
}
```

---

## Image Assets

Images must use `@2x.png` suffix and be in `Common/UI/Custom/`:
- Reference: `TexturePath: "MyImage.png"`
- File: `src/main/resources/Common/UI/Custom/MyImage@2x.png`

Ensure `"IncludesAssetPack": true` in `manifest.json`.

---

## Threading (CRITICAL)

**UI operations MUST run on the world thread** or the game will crash.

### For Commands

```java
public class MyCommand extends AbstractAsyncCommand {
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        if (context.sender() instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            
            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                // Open page on world thread
                playerComponent.getPageManager().setPage(ref, store, new MyPage(playerRef));
            }, world);
        }
        return CompletableFuture.completedFuture(null);
    }
}
```

### For HUDs

```java
world.execute(() -> {
    MyHud hud = new MyHud(playerRef);
    hud.show();
});
```

---

## Example: Complete Interactive Page

### Pages/MyPage.ui

```ui
$Common = "../Common.ui";

Group #Root {
    Anchor: (Full: 0);
    Background: PatchStyle(Color: #000000A0);
    LayoutMode: CenterMiddle;
    
    Group #Container {
        Anchor: (Width: 400, Height: 300);
        Background: PatchStyle(Color: #1a1a2eE0, Border: 4);
        LayoutMode: Top;
        Padding: (Full: 20);
        
        Label #Title {
            Text: "My Page";
            Style: (FontSize: 24, RenderBold: true, Alignment: Center);
            Anchor: (Height: 40);
        }
        
        TextField #SearchInput {
            Anchor: (Height: 30);
            PlaceholderText: "Search...";
        }
        
        Group #Results {
            Anchor: (Full: 0);
            LayoutMode: TopScrolling;
            ScrollbarStyle: $Common.@DefaultScrollbarStyle;
        }
        
        Group #ButtonRow {
            LayoutMode: Right;
            Anchor: (Height: 40);
            
            Button #CancelBtn {
                Text: "Cancel";
                Anchor: (Width: 100);
            }
            
            Button #SaveBtn {
                Text: "Save";
                Anchor: (Width: 100);
            }
        }
    }
}
```

### Java Implementation

```java
public class MyPage extends InteractiveCustomUIPage<MyPage.EventData> {
    
    public MyPage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventData.CODEC);
    }
    
    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, 
                      UIEventBuilder events, Store<EntityStore> store) {
        cmd.append("Pages/MyPage.ui");
        
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
            EventData.of("@Query", "#SearchInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
            EventData.of("Action", "save"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
            EventData.of("Action", "cancel"), false);
    }
    
    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EventData data) {
        if ("cancel".equals(data.action)) {
            close(ref, store);
            return;
        }
        if ("save".equals(data.action)) {
            // Save logic...
            close(ref, store);
            return;
        }
        if (data.query != null) {
            updateResults(data.query);
        }
        sendUpdate(null, false);
    }
    
    private void close(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().setPage(ref, store, Page.None);
    }
    
    private void updateResults(String query) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.clear("#Results");
        // Add filtered results...
        for (String result : getFilteredResults(query)) {
            cmd.appendInline("#Results", 
                String.format("Label { Text: \"%s\"; Anchor: (Height: 24); }", result));
        }
        sendUpdate(cmd, false);
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

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `Failed to apply Custom UI HUD commands` | Syntax error in .ui file; enable Diagnostic Mode in Hytale settings |
| `Could not find document for Custom UI Append` | Wrong path or file not in `Common/UI/Custom/` |
| `Unknown node type: X` | Element type not supported or misspelled |
| Page stuck on "Loading…" | Forgot `sendUpdate()` after handling events |
| Client disconnect on UI open | Not running on world thread |
| Texture not showing | Missing `@2x.png` suffix or wrong path |
| Events not firing | Selector doesn't match element ID |

---

## Checklist

1. ✅ Place .ui files in `resources/Common/UI/Custom/`
2. ✅ Add `"IncludesAssetPack": true` to `manifest.json`
3. ✅ Image files must end with `@2x.png`
4. ✅ Run UI operations on world thread
5. ✅ Call `sendUpdate()` after handling events in InteractiveCustomUIPage
6. ✅ Use proper selectors with `#` prefix
7. ✅ Enable Diagnostic Mode for debugging

---

## References

- Hytale Modding Docs: https://hytalemodding.dev/en/docs/guides/plugin/ui
- Hytale UI Builder (visual editor): https://hytale.ellie.au/
