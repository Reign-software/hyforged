# UI Modding Reference

This document provides comprehensive documentation for creating custom user interfaces in Hytale plugins. UI is essential for creating interactive experiences, admin panels, HUDs, and game menus.

## Overview

Hytale uses `.ui` files to define UI layouts, similar to HTML/CSS. The server sends UI definitions to clients, which render them. There are two main UI paradigms:

1. **HUDs** — Always-visible overlays (health bars, hotbars, custom indicators)
2. **Pages** — Modal interfaces that capture player focus (menus, dialogs, forms)

> **Note**: Hytale is planning to migrate to NoesisGUI in the future, but `.ui` files remain the current method.

---

## Important Setup

### Manifest Configuration
Your `manifest.json` must include:
```json
{
  "IncludesAssetPack": true
}
```

### File Location
All `.ui` files must be placed in:
```
resources/Common/UI/Custom/
```

### Debugging
Enable **Diagnostic Mode** in Hytale settings (under General) for detailed UI error messages.

---

## .ui File Syntax

### Basic Structure
UI files use a declarative syntax with nested elements:

```ui
Group {
  LayoutMode: Center;
  
  Label #MyLabel {
    Style: (FontSize: 32, Alignment: Center);
    Text: "Hello World";
    Padding: (Full: 10);
  }
}
```

### UI Elements

| Element | Description |
|---------|-------------|
| `Group` | Container element (like HTML `<div>`) |
| `Label` | Text display |
| `TextField` | Text input field |
| `Button` | Clickable button |
| `Timer` | Timer label |
| `ColorPicker` | Color selection UI |

### Element IDs
Elements are identified using `#` prefix:
```ui
TextField #MyInput {
  Style: $Common.@DefaultInputFieldStyle;
  Background: $Common.@InputBoxBackground;
  Anchor: (Top: 10, Width: 200, Height: 50);
}
```

### Variables
Define reusable styles with `@` prefix:
```ui
@MyTex = PatchStyle(TexturePath: "MyBackground.png");
@ButtonStyle = (FontSize: 18, Bold: true);
```

### Textures
Load textures relative to the `.ui` file:
```ui
@Background = PatchStyle(TexturePath: "MyBackground.png");

Group {
  Background: @Background;
}
```

### Including Other Files
Import shared UI definitions:
```ui
$Common = "Common.ui";

Group {
  Style: $Common.@DefaultInputFieldStyle;
}
```

### Anchoring & Layout

```ui
Anchor: (Top: 10, Left: 20, Width: 200, Height: 50);
Anchor: (Width: 800, Height: 1000);
LayoutMode: Center;  // Center, Top, Left, Right, Bottom
```

---

## Key Classes

### Core UI Classes

| Class | Package | Purpose |
|-------|---------|---------|
| `CustomUIHud` | `com.hypixel.hytale.server.core.entity.entities.player.hud` | Base class for HUDs |
| `CustomUIPage` | `com.hypixel.hytale.server.core.entity.entities.player.pages` | Base class for non-interactive pages |
| `InteractiveCustomUIPage<T>` | `com.hypixel.hytale.server.core.entity.entities.player.pages` | Base for interactive pages with event data |
| `HudManager` | `com.hypixel.hytale.server.core.entity.entities.player.hud` | Manages player HUD state |
| `PageManager` | `com.hypixel.hytale.server.core.entity.entities.player.pages` | Manages player page state |

### UI Builders

| Class | Package | Purpose |
|-------|---------|---------|
| `UICommandBuilder` | `com.hypixel.hytale.server.core.ui.builder` | Builds UI commands (append, set, clear) |
| `UIEventBuilder` | `com.hypixel.hytale.server.core.ui.builder` | Builds event bindings |
| `EventData` | `com.hypixel.hytale.server.core.ui.builder` | Creates event data mappings |

### Enums

| Enum | Values |
|------|--------|
| `CustomPageLifetime` | `CantClose`, `CanDismiss`, `CanDismissOrCloseThroughInteraction` |
| `CustomUIEventBindingType` | `Activating`, `RightClicking`, `DoubleClicking`, `MouseEntered`, `MouseExited`, `ValueChanged`, `ElementReordered`, `Validating`, `Dismissing`, `FocusGained`, `FocusLost`, `KeyDown`, `MouseButtonReleased`, `SlotClicking`, `SlotDoubleClicking`, `SlotMouseEntered`, `SlotMouseExited`, `DragCancelled`, `Dropped`, `SlotMouseDragCompleted`, `SlotMouseDragExited`, `SlotClickReleaseWhileDragging`, `SlotClickPressWhileDragging`, `SelectedTabChanged` |

---

## HUDs (Heads-Up Display)

HUDs are always-visible UI elements that don't capture player input.

### Creating a CustomUIHud

```java
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class MyHud extends CustomUIHud {
    
    public MyHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
    }
    
    @Override
    protected void build(@Nonnull UICommandBuilder uiCommandBuilder) {
        // File path relative to resources/Common/UI/Custom/
        uiCommandBuilder.append("MyHud.ui");
    }
}
```

### Showing & Hiding HUDs

```java
// Get HudManager from Player component
Player player = store.getComponent(ref, Player.getComponentType());
HudManager hudManager = player.getHudManager();

// Show custom HUD
hudManager.setCustomHud(playerRef, new MyHud(playerRef));

// Hide custom HUD
hudManager.setCustomHud(playerRef, null);

// Hide default Hytale HUD components
hudManager.hideHudComponents(playerRef, HudComponent.Health, HudComponent.Hotbar);

// Reset to default HUD
hudManager.resetHud(playerRef);
```

### Updating HUD Dynamically

```java
public class MyHud extends CustomUIHud {
    
    public void updateScore(int score) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#ScoreLabel.Text", String.valueOf(score));
        update(false, builder);  // false = don't clear existing UI
    }
}
```

---

## Pages (Modal UI)

Pages are full-screen or modal UI that captures player input and unlocks the cursor.

### Non-Interactive Pages (CustomUIPage)

For display-only pages without user input:

```java
public class InfoPage extends CustomUIPage {
    
    public InfoPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss);
    }
    
    @Override
    public void build(@Nonnull Ref<EntityStore> ref, 
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder, 
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/InfoPage.ui");
    }
}
```

### Interactive Pages (InteractiveCustomUIPage)

For pages that receive user input and events:

#### Step 1: Define Event Data Class

```java
public class MyPage extends InteractiveCustomUIPage<MyPage.Data> {
    
    public static class Data {
        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
            .append(new KeyedCodec<>("@MyInput", Codec.STRING), 
                    (data, value) -> data.inputValue = value, 
                    data -> data.inputValue)
            .add()
            .append(new KeyedCodec<>("ButtonId", Codec.STRING),
                    (data, value) -> data.buttonId = value,
                    data -> data.buttonId)
            .add()
            .build();
        
        private String inputValue;
        private String buttonId;
        
        public String getInputValue() { return inputValue; }
        public String getButtonId() { return buttonId; }
    }
}
```

#### Step 2: Implement the Page

```java
public class MyPage extends InteractiveCustomUIPage<MyPage.Data> {
    
    public MyPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
    }
    
    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/MyPage.ui");
        
        // Bind input field changes
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#MyInput",
            EventData.of("@MyInput", "#MyInput.Value"),
            false  // locksInterface
        );
        
        // Bind button click
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SubmitButton",
            EventData.of("ButtonId", "submit")
        );
    }
    
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);
        
        if ("submit".equals(data.getButtonId())) {
            // Handle submit button
            System.out.println("Input value: " + data.getInputValue());
        }
        
        // CRITICAL: Always call sendUpdate() or close the page
        // Otherwise client shows "Loading..." forever
        sendUpdate();
    }
}
```

### Opening & Closing Pages

```java
Player player = store.getComponent(ref, Player.getComponentType());
PageManager pageManager = player.getPageManager();

// Open custom page
pageManager.openCustomPage(ref, store, new MyPage(playerRef));

// Close page (return to game)
pageManager.setPage(ref, store, Page.None);
```

---

## UICommandBuilder Methods

| Method | Description |
|--------|-------------|
| `append(String documentPath)` | Append a .ui file |
| `append(String selector, String documentPath)` | Append .ui file into selector |
| `appendInline(String selector, String document)` | Append inline UI definition |
| `clear(String selector)` | Clear children of selector |
| `remove(String selector)` | Remove element at selector |
| `set(String selector, String value)` | Set string property |
| `set(String selector, int value)` | Set integer property |
| `set(String selector, float value)` | Set float property |
| `set(String selector, boolean value)` | Set boolean property |
| `set(String selector, Message message)` | Set localized message |
| `setNull(String selector)` | Set property to null |
| `insertBefore(String selector, String documentPath)` | Insert before element |
| `insertBeforeInline(String selector, String document)` | Insert inline before element |

### Selector Syntax

```java
// Element by ID
"#MyLabel.Text"

// Indexed child
"#WarpList[0]"

// Nested element
"#WarpList[0] #Name.Text"

// Property access
"#MyInput.Value"
"#MyButton.Visible"
```

---

## Event Binding

### Event Types

| Event | Trigger |
|-------|---------|
| `Activating` | Button/element clicked |
| `RightClicking` | Right mouse click |
| `DoubleClicking` | Double click |
| `ValueChanged` | Input value changed |
| `MouseEntered` | Mouse enters element |
| `MouseExited` | Mouse leaves element |
| `FocusGained` | Element gains focus |
| `FocusLost` | Element loses focus |
| `KeyDown` | Key pressed while focused |
| `Dismissing` | Page being dismissed |
| `SelectedTabChanged` | Tab selection changed |

### EventData Mapping

```java
// Map element value to codec field
EventData.of("@CodecFieldName", "#ElementId.Value")

// Map static value
EventData.of("ActionType", "delete")

// Multiple mappings
EventData.of("@Field1", "#Input1.Value")
         .and("@Field2", "#Input2.Value")
```

---

## Dynamic UI Updates

### Updating Labels

```java
public void updateText(String newText) {
    UICommandBuilder builder = new UICommandBuilder();
    builder.set("#MyLabel.Text", newText);
    update(false, builder);  // false = don't clear
}
```

### Building Dynamic Lists

```java
private void buildList(UICommandBuilder builder, UIEventBuilder eventBuilder, List<String> items) {
    builder.clear("#ItemList");
    
    for (int i = 0; i < items.size(); i++) {
        String selector = "#ItemList[" + i + "]";
        builder.append("#ItemList", "Components/ListItem.ui");
        builder.set(selector + " #ItemName.Text", items.get(i));
        
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            selector,
            EventData.of("SelectedItem", items.get(i))
        );
    }
}
```

### Updating Interactive Pages

```java
@Override
public void handleDataEvent(..., Data data) {
    // Process event...
    
    // Update UI
    UICommandBuilder builder = new UICommandBuilder();
    UIEventBuilder eventBuilder = new UIEventBuilder();
    
    builder.set("#StatusLabel.Text", "Updated!");
    buildList(builder, eventBuilder, newItems);
    
    sendUpdate(builder, eventBuilder, false);
}
```

---

## Common Patterns

### Search/Filter Pattern

```java
public class SearchablePage extends InteractiveCustomUIPage<SearchablePage.Data> {
    private String searchQuery = "";
    
    @Override
    public void build(...) {
        commandBuilder.append("Pages/SearchPage.ui");
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#SearchInput",
            EventData.of("@SearchQuery", "#SearchInput.Value")
        );
        buildResults(commandBuilder, eventBuilder);
    }
    
    private void buildResults(UICommandBuilder cmd, UIEventBuilder evt) {
        cmd.clear("#Results");
        List<String> filtered = items.stream()
            .filter(i -> searchQuery.isEmpty() || 
                        i.toLowerCase().contains(searchQuery))
            .toList();
        // Build list...
    }
    
    @Override
    public void handleDataEvent(..., Data data) {
        if (data.searchQuery != null) {
            this.searchQuery = data.searchQuery.trim().toLowerCase();
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder evt = new UIEventBuilder();
            buildResults(cmd, evt);
            sendUpdate(cmd, evt, false);
        }
    }
}
```

### Confirmation Dialog Pattern

```java
public class ConfirmDialog extends InteractiveCustomUIPage<ConfirmDialog.Data> {
    private final Runnable onConfirm;
    private final Runnable onCancel;
    
    @Override
    public void handleDataEvent(..., Data data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().setPage(ref, store, Page.None);
        
        if ("confirm".equals(data.action)) {
            onConfirm.run();
        } else {
            onCancel.run();
        }
    }
}
```

---

## Libraries & Tools

### Visual UI Editor
**Hytale UI Builder** — Browser-based visual editor
- URL: https://hytale.ellie.au/
- Generates `.ui` files visually
- Supports all primitives and macros

### HyUI Library
**Fluent API for UI creation** — Create UI programmatically from Java
- CurseForge: https://www.curseforge.com/hytale/mods/hyui
- Features:
  - Builder pattern API
  - HYUIML (HTML-like syntax)
  - Lambda event handlers
  - Multi-HUD support
  - Periodic refresh

```java
// HyUI Example
new PageBuilder(playerRef)
    .fromFile("Pages/MyMenu.ui")
    .addElement(new GroupBuilder()
        .withId("MainContainer")
        .inside("#Content")
        .addChild(ButtonBuilder.textButton()
            .withText("Click Me!")
            .addEventListener(CustomUIEventBindingType.Activating, (ctx) -> {
                playerRef.sendMessage(Message.raw("Clicked!"));
            })))
    .open(store);
```

### MultipleHUD Library
**Display multiple HUDs simultaneously**
- CurseForge: https://www.curseforge.com/hytale/mods/multiplehud
- Maven: https://maven.hytale-modding.info/

```java
MultipleHUD.getInstance().setCustomHud(player, playerRef, "Hud1", new MyHud1());
MultipleHUD.getInstance().setCustomHud(player, playerRef, "Hud2", new MyHud2());
```

---

## Example Repositories

| Repository | Description |
|------------|-------------|
| [Hytale-Sandbox-Plugin (ui-pages branch)](https://github.com/underscore95/Hytale-Sandbox-Plugin/tree/ui-pages) | Simple InteractiveCustomUIPage example |
| [Hytale-Sandbox-Plugin (multiple-huds branch)](https://github.com/underscore95/Hytale-Sandbox-Plugin/tree/multiple-huds) | MultipleHUD usage example |
| [AdminUI](https://github.com/Buuz135/AdminUI) | Complex admin panel with multiple interactive GUIs |

---

## Video Tutorials

- **CustomUIHud Tutorial**: https://www.youtube.com/watch?v=u4pGShklEKs
- **InteractiveCustomUIPage Tutorial**: https://www.youtube.com/watch?v=NOFWQt9wEbk
- **Useful Information & Resources**: https://www.youtube.com/watch?v=AaNk_g9vb50

---

## Common Issues

### "Failed to apply Custom UI HUD commands"
- Your `.ui` file has a syntax error
- Enable **Diagnostic Mode** in Hytale settings for details

### "Could not find document XXXXX for Custom UI Append command"
- File path is incorrect
- Ensure file is in `resources/Common/UI/Custom/`
- Check the path in `uiCommandBuilder.append()` matches

### Page shows "Loading..." forever
- You must call `sendUpdate()` after handling events
- Or close the page with `pageManager.setPage(ref, store, Page.None)`

### UI not updating
- Ensure `update(false, builder)` is called for HUDs
- For pages, use `sendUpdate(builder, eventBuilder, false)`
- The `false` parameter means "don't clear existing UI"

---

## See Also

- [key-classes.md](key-classes.md) — Core class reference
- [plugin-dev-cheatsheet.md](plugin-dev-cheatsheet.md) — Quick reference
- [HytaleModding.dev UI Guide](https://hytalemodding.dev/en/docs/guides/plugin/ui) — Official community guide
