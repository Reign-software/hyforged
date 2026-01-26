---
name: hytale-ui-modding
description: Comprehensive guidance for Hytale plugin UI modding using .ui files, HyUI library (PageBuilder, HudBuilder, HYUIML), CustomUIHud, CustomUIPage, and InteractiveCustomUIPage. Use when creating or updating custom HUDs/pages, using HyUI builders, writing HYUIML markup, binding UI events, or troubleshooting UI issues.
---

# Hytale UI Modding Skill

Use this skill when working on Hytale plugin UI. This covers both raw `.ui` files and the **HyUI library** which provides a fluent builder API and HYUIML (HTML/CSS-like markup).

## Quick Reference

| Approach | When to Use |
|----------|-------------|
| **HyUI + HYUIML** | Complex UIs, rapid development, multi-HUD systems, interactive pages |
| **Raw .ui files** | Simple static UIs, performance-critical scenarios, learning Hytale UI |
| **HyUI + .ui files** | Load base layout from .ui, add dynamic elements via builders |

---

## HyUI Library (Recommended for Complex UIs)

HyUI is a fluent, builder-based library that simplifies UI creation. It provides:
- **HYUIML**: HTML/CSS-like markup language
- **Fluent Builder API**: Clean Java API for UI construction
- **Multi-HUD System**: Multiple independent HUDs without conflicts
- **Event Handling**: Lambda-based event listeners with `UIContext`
- **Template Processor**: Variables, loops, conditionals, reusable components

### Installation (Maven)

```xml
<repositories>
    <repository>
        <id>cursemaven</id>
        <url>https://www.cursemaven.com</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>curse.maven</groupId>
        <artifactId>hyui-1431415</artifactId>
        <version>7522546</version> <!-- File ID from CurseForge -->
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Installation (Gradle)

```gradle
repositories {
    maven { url "https://www.cursemaven.com" }
}

dependencies {
    implementation "curse.maven:hyui-1431415:<file-id>"
}
```

---

## PageBuilder - Full-Screen Interactive Pages

Pages block game input and show a modal interface.

### Quick Start: Opening a Page

```java
String html = """
    <div class="page-overlay">
        <div class="container" data-hyui-title="My Page">
            <div class="container-contents">
                <p>Hello Hytale!</p>
                <button id="myBtn">Click Me</button>
            </div>
        </div>
    </div>
    """;

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .addEventListener("myBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
        playerRef.sendMessage(Message.raw("Button clicked!"));
    })
    .open(store);
```

### Content Sources

#### 1. HYUIML (HTML) - Recommended for Complex UIs
```java
PageBuilder.pageForPlayer(playerRef)
    .fromHtml("""
        <div class="page-overlay">
            <div class="decorated-container" data-hyui-title="Settings">
                <div class="container-contents">
                    <p>Settings content here</p>
                </div>
            </div>
        </div>
    """)
    .open(store);
```

#### 2. Loading from .ui Files
```java
PageBuilder.pageForPlayer(playerRef)
    .fromFile("Pages/MyPage.ui")
    .open(store);
```
> **Note**: Elements in .ui files cannot use `.addEventListener`. Use `.editElement` for raw commands.

#### 3. Manual Builder API
```java
PageBuilder.detachedPage()
    .withLifetime(CustomPageLifetime.CanDismiss)
    .addElement(PageOverlayBuilder.pageOverlay()
        .withId("MyOverlay")
        .addChild(ContainerBuilder.container()
            .withTitleText("Manual UI")
            .addContentChild(LabelBuilder.label().withText("Built with code!"))
        )
    )
    .addElement(ButtonBuilder.backButton())
    .open(playerRef, store);
```

### Detached Pages (Pre-configured)
```java
// Prepare configuration without a player
PageBuilder builder = PageBuilder.detachedPage()
    .fromHtml("<p>Hello World</p>")
    .withLifetime(CustomPageLifetime.CanDismiss);

// Later, open for a specific player
builder.open(playerRef, store);
```

### Event Listeners and UIContext

```java
.addEventListener("my-button", CustomUIEventBindingType.Activating, (data, ctx) -> {
    // Access the page
    ctx.getPage().ifPresent(page -> page.close());
    
    // Get values from input elements
    Optional<String> username = ctx.getValue("username-input", String.class);
    
    // Update elements dynamically
    ctx.getById("label", LabelBuilder.class).ifPresent(lb -> {
        lb.withText("Updated!");
        ctx.updatePage(true);
    });
});
```

### Updating Page Elements at Runtime

```java
PageBuilder.detachedPage()
    .fromHtml("""
        <p id="counter">Clicks: 0</p>
        <button id="btn">Click Me!</button>
    """)
    .addEventListener("btn", CustomUIEventBindingType.Activating, (data, ctx) -> {
        int newClicks = clicks.incrementAndGet();
        ctx.getById("counter", LabelBuilder.class).ifPresent(lb -> {
            lb.withText("Clicks: " + newClicks);
            ctx.updatePage(true);
        });
    })
    .open(playerRef, store);
```

---

## HudBuilder - Persistent On-Screen Elements

HUDs are always-visible overlays. HyUI manages the multi-HUD system automatically.

### Quick Start: Showing a HUD

```java
HudBuilder.hudForPlayer(playerRef)
    .fromHtml("<div style='anchor-top: 10; anchor-left: 10;'><p>Health: 100</p></div>")
    .show(store);
```

### Content Sources

```java
// From HYUIML
HudBuilder.hudForPlayer(playerRef)
    .fromHtml("<div style='anchor-top: 10;'><p>Hello!</p></div>")
    .show(store);

// From .ui file
HudBuilder.hudForPlayer(playerRef)
    .fromFile("Huds/MyHud.ui")
    .show(store);

// Manual builders
HudBuilder.hudForPlayer(playerRef)
    .addElement(LabelBuilder.label()
        .withText("Manual HUD")
        .withAnchor(new HyUIAnchor().setTop(10).setLeft(10)))
    .show(store);
```

### Multi-HUD System

HyUI automatically manages multiple HUDs:
- Multiple independent HUD elements (minimap, quest tracker, notifications) run simultaneously
- Each HUD has its own refresh rate, elements, and event listeners
- HUDs don't interfere with each other or other HyUI-based mods
- Just build your HUD and call `.show()` — HyUI handles composition

### Periodic Refreshing

```java
HudBuilder.hudForPlayer(playerRef)
    .withRefreshRate(1000) // Refresh every 1 second
    .onRefresh(hud -> {
        hud.getById("timer", LabelBuilder.class).ifPresent(label -> {
            label.withText("Time: " + System.currentTimeMillis());
        });
    })
    .show(store);
```

### Visibility Control

```java
hud.hide();   // Hides the HUD
hud.unhide(); // Shows it again
hud.remove(); // Completely removes from multi-HUD manager
hud.readd();  // Re-adds it later
```

### Thread Safety (CRITICAL)

**`.show()` MUST be called on the world thread!**

```java
world.execute(() -> {
    HudBuilder.hudForPlayer(playerRef)
        .fromHtml("<div>Welcome!</div>")
        .show(store);
});
```

### Showing HUD on Player Join

```java
public void onPlayerReady(PlayerReadyEvent event) {
    var player = event.getPlayer();
    if (player == null) return;
    
    Ref<EntityStore> ref = player.getReference();
    if (ref == null || !ref.isValid()) return;
    
    Store<EntityStore> store = ref.getStore();
    World world = store.getExternalData().getWorld();
    
    world.execute(() -> {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        HudBuilder.detachedHud()
            .fromHtml("<div style='anchor-top: 10;'><p>Welcome!</p></div>")
            .show(playerRef, store);
    });
}
```

---

## HYUIML - HTML/CSS Markup Language

HYUIML is a lightweight HTML-like syntax that compiles to HyUI builders.

### Supported Tags

| HTML Tag | HyUI Builder | Notes |
|----------|--------------|-------|
| `<div>` | `GroupBuilder` | Layout containers |
| `<div class="page-overlay">` | `PageOverlayBuilder` | Full-screen overlay |
| `<div class="container">` | `ContainerBuilder` | Hytale window frame |
| `<div class="decorated-container">` | `ContainerBuilder` | Styled frame variant |
| `<div class="container-contents">` | - | Content area inside container |
| `<div class="tab-content">` | `TabContentBuilder` | Tab content linked to tab ID |
| `<div class="item-grid">` | `ItemGridBuilder` | Scrollable item grid |
| `<p>`, `<label>` | `LabelBuilder` | Text labels |
| `<button>` | `ButtonBuilder` | Standard buttons |
| `<button class="back-button">` | `ButtonBuilder` | Themed back button |
| `<button class="secondary-button">` | `ButtonBuilder` | Secondary style |
| `<button class="tertiary-button">` | `ButtonBuilder` | Tertiary style |
| `<input type="text">` | `TextFieldBuilder` | Text input |
| `<input type="password">` | `TextFieldBuilder` | Masked input |
| `<input type="number">` | `NumberFieldBuilder` | Numeric input |
| `<input type="range">` | `SliderBuilder` | Slider control |
| `<input type="checkbox">` | `CheckBoxBuilder` | Toggle switch |
| `<input type="color">` | `ColorPickerBuilder` | Color selector |
| `<progress>` | `ProgressBarBuilder` | Progress bar |
| `<progress class="circular-progress">` | `ProgressBarBuilder` | Circular variant |
| `<img>` | `ImageBuilder` | Static images |
| `<img class="dynamic-image">` | `DynamicImageBuilder` | Runtime-downloaded PNG |
| `<hyvatar>` | `HyvatarImageBuilder` | Player avatar renders |
| `<select>` | `DropdownBoxBuilder` | Dropdown with `<option>` children |
| `<nav class="tabs">` | `TabNavigationBuilder` | Tab bar |
| `<sprite>` | `SpriteBuilder` | Animated sprite |
| `<span class="item-icon">` | `ItemIconBuilder` | Item icon |
| `<span class="item-slot">` | `ItemSlotBuilder` | Full item slot |

### Supported Attributes

#### Standard Attributes
- `id` — Element ID for Java access and event listeners
- `class` — CSS classes
- `value` — Initial value for inputs
- `min`, `max`, `step` — Slider range
- `checked` — Checkbox state (`true`/`false`)
- `placeholder` — Hint text for inputs
- `maxlength` — Character limit
- `readonly` — Read-only state
- `width`, `height` — Image dimensions

#### HyUI-Specific Attributes (`data-hyui-*`)
- `data-hyui-title` — Container/overlay header title
- `data-hyui-tooltiptext` — Tooltip text
- `data-hyui-item-id` — Item ID for icons
- `data-hyui-tab-id` — Link content to tab ID
- `data-hyui-tab-nav` — Target specific tab navigation
- `data-hyui-bar-texture-path` — Progress bar fill texture
- `data-hyui-color` — Progress bar color (hex)
- `data-hyui-direction` — Progress bar direction (`start`/`end`)
- `data-hyui-alignment` — Progress orientation (`horizontal`/`vertical`)
- `data-hyui-style` — Arbitrary style keys (e.g., `SlotSpacing: 6`)

#### Sprite Attributes
- `data-hyui-frame-width`, `data-hyui-frame-height` — Frame dimensions
- `data-hyui-frame-per-row`, `data-hyui-frame-count` — Animation layout
- `data-hyui-fps` — Animation speed

#### Item Grid Attributes
- `data-hyui-slots-per-row` — Slots per row
- `data-hyui-are-items-draggable` — Drag behavior
- `data-hyui-show-scrollbar` — Scrollbar visibility

### CSS Styling

Include a `<style>` block in your HYUIML:

```html
<style>
    .header {
        color: #ff0000;
        font-weight: bold;
        font-size: 24;
    }
    #my-button {
        flex-weight: 1;
        anchor-width: 200;
    }
</style>
<div class="header">Title</div>
<button id="my-button">Click Me</button>
```

#### Supported CSS Properties

| Property | Values/Notes |
|----------|--------------|
| `color` | Hex colors (`#FFFFFF`) |
| `font-size` | Numeric value |
| `font-weight` | `bold`, `normal` |
| `text-transform` | `uppercase`, `none` |
| `text-align` | `top`, `bottom`, `left`, `right`, `center`, `middle` |
| `vertical-align` | `top`, `bottom`, `center` |
| `horizontal-align` | `left`, `right`, `center` |
| `visibility` | `hidden`, `shown` |
| `display` | `none`, `block` |
| `flex-weight` | Numeric weight for layout |
| `layout-mode` | `Top`, `Left`, `Center`, etc. |
| `anchor-*` | `anchor-left`, `anchor-top`, `anchor-width`, `anchor-height`, etc. |
| `padding`, `padding-*` | Padding values |
| `background-image` | `url('path.png')` with optional border values |
| `background-color` | Hex color with optional border values |
| `hyui-style-reference` | Reference styles from .ui files |

### Image Assets

Images are relative to `Common/UI/Custom/`. Hytale requires `@2x.png` suffix:
- Use: `<img src="lizard.png"/>`
- File: `src/main/resources/Common/UI/Custom/lizard@2x.png`

### Dynamic Images

Download PNGs at runtime (limited to 10 per page):
```html
<img class="dynamic-image" src="https://example.com/image.png" />
```

### Hyvatar Integration

Render player avatars via Hyvatar.io:
```html
<hyvatar username="PlayerName" render="head" size="256" rotate="45"></hyvatar>
```
- `render`: `head`, `full`, or `cape`
- `size`: 64-2048
- `rotate`: 0-360 degrees

### Tab Navigation

```html
<nav id="main-tabs" class="tabs"
     data-tabs="tab1:Tab One,tab2:Tab Two"
     data-selected="tab1">
</nav>

<div class="tab-content" data-hyui-tab-id="tab1">
    <p>Content for Tab One</p>
</div>

<div class="tab-content" data-hyui-tab-id="tab2">
    <p>Content for Tab Two</p>
</div>
```

---

## Template Processor

For complex, data-driven UIs, use the Template Processor.

### Variable Interpolation

```java
TemplateProcessor template = new TemplateProcessor()
    .setVariable("playerName", playerRef.getUsername())
    .setVariable("playerLevel", 42);
```

```html
<p>Player: {{$playerName}}</p>
<p>Level: {{$playerLevel}}</p>
<p>Missing: {{$missing|Default Value}}</p>
<p>Uppercase: {{$playerName|upper}}</p>
```

### Each Loops

```java
template.setVariable("items", itemList);
```

```html
{{#each items}}
    <p>{{$name}} - {{$quantity}}</p>
{{/each}}
```

### Conditionals

```html
{{#if isAdmin}}
    <p>Admin mode</p>
{{else}}
    <p>Standard mode</p>
{{/if}}

{{#if power >= 10 && rarity != Common}}
    <p>Strong item</p>
{{/if}}

{{#if tags contains "rare" || rarity == Epic}}
    <p>Highlight</p>
{{/if}}
```

Operators: `==`, `!=`, `>`, `<`, `>=`, `<=`, `&&`, `||`, `!`, `contains`

### Reusable Components

```java
template.registerComponent("statCard", """
    <div style="background-color: #2a2a3e; padding: 10;">
        <p style="color: #888;">{{$label}}</p>
        <p style="font-weight: bold;">{{$value}}</p>
    </div>
""");
```

```html
{{@statCard:label=Health,value=100}}
{{@statCard:label=Mana,value=50}}
```

### Using with PageBuilder

```java
TemplateProcessor template = new TemplateProcessor()
    .setVariable("playerName", playerRef.getUsername())
    .registerComponent("header", "<p style='font-size: 24;'>{{$title}}</p>");

String html = template.process("""
    <div class="page-overlay">
        {{@header:title=Welcome {{$playerName}}}}
        <p>Your adventure awaits!</p>
    </div>
""");

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .open(store);
```

---

## HyUI Builder Reference

### Core Builders

| Builder | Purpose |
|---------|---------|
| `PageBuilder` | Full-screen interactive pages |
| `HudBuilder` | Persistent HUD elements |
| `GroupBuilder` | Layout containers |
| `ContainerBuilder` | Hytale window frames |
| `PageOverlayBuilder` | Full-screen overlays |
| `TabNavigationBuilder` | Tab bars |
| `TabContentBuilder` | Tab content sections |

### Input Builders

| Builder | Purpose |
|---------|---------|
| `TextFieldBuilder` | Text input |
| `NumberFieldBuilder` | Numeric input |
| `SliderBuilder` | Range slider |
| `CheckBoxBuilder` | Toggle switch |
| `ColorPickerBuilder` | Color picker |
| `DropdownBoxBuilder` | Dropdown selection |

### Display Builders

| Builder | Purpose |
|---------|---------|
| `LabelBuilder` | Text labels |
| `ButtonBuilder` | Buttons (text, back, secondary, tertiary) |
| `ImageBuilder` | Static images |
| `DynamicImageBuilder` | Runtime-downloaded images |
| `HyvatarImageBuilder` | Player avatar renders |
| `ProgressBarBuilder` | Progress bars (linear/circular) |
| `SpriteBuilder` | Animated sprites |
| `ItemIconBuilder` | Item icons |
| `ItemSlotBuilder` | Item slots |
| `ItemGridBuilder` | Scrollable item grids |
| `TimerLabelBuilder` | Timer display |

### Common Builder Methods

```java
.withId("element-id")           // Set ID for event listeners
.withAnchor(new HyUIAnchor())   // Position and size
.withStyle(new HyUIStyle())     // Styling
.withVisible(true/false)        // Visibility
.addChild(builder)              // Add child element
.addEventListener(type, handler) // Event listener
```

---

## Raw .ui Files (Low-Level)

For simple UIs or when HyUI is unavailable.

### File Location
```
src/main/resources/Common/UI/Custom/
```

### Basic Syntax

```ui
// Comment
$Common = "../Common.ui";

Group #Root {
    Anchor: (Full: 0);
    LayoutMode: Top;
    Background: PatchStyle(Color: #1a1a1e);
    
    Label #Title {
        Text: "Hello World";
        Style: (FontSize: 24, RenderBold: true);
    }
    
    Button #MyButton {
        Content: "Click Me";
    }
}
```

### Valid Element Types

- `Group` — Container (like div)
- `Label` — Text display
- `Button` — Clickable button
- `TextField` — Text input
- `Timer` — Timer display
- `ColorPicker` — Color selector

### Style Properties

```ui
Style: (FontSize: 14, RenderBold: true, Alignment: Center);
```

### Anchor Properties

```ui
Anchor: (Left: 10, Top: 10, Width: 200, Height: 50);
Anchor: (Full: 0);  // Fill parent
Anchor: (HCenter: 0, VCenter: 0, Width: 300);  // Centered
```

### PatchStyle for Backgrounds

```ui
Background: PatchStyle(Color: #1a1a1eE0);
Background: PatchStyle(TexturePath: "MyTexture.png");
Background: PatchStyle(TexturePath: "MyTexture.png", Border: 4);
```

---

## Thread Safety (CRITICAL)

**UI operations MUST run on the world thread** or the game will crash.

### For Commands Opening UI

```java
public class MyUICommand extends AbstractAsyncCommand {
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        if (context.sender() instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            
            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                PageBuilder.pageForPlayer(playerRef)
                    .fromHtml("<p>Hello!</p>")
                    .open(store);
            }, world);
        }
        return CompletableFuture.completedFuture(null);
    }
}
```

### For HUDs

```java
world.execute(() -> {
    HudBuilder.hudForPlayer(playerRef).fromHtml("...").show(store);
});
```

> **Note**: Event handlers inside HyUI pages/HUDs already run on the correct thread.

---

## Common Issues

| Issue | Solution |
|-------|----------|
| `Failed to apply Custom UI HUD commands` | Syntax error in .ui file; check Diagnostic Mode |
| `Could not find document for Custom UI Append` | Wrong path or file not in `Common/UI/Custom/` |
| `Unknown node type: X` | Element type not supported (e.g., `ScrollViewer`) |
| `Expected end of file` | XML wrapper or invalid syntax in .ui file |
| Client disconnect on UI open | Not running on world thread |
| Dynamic images not loading | URL issues or hit 10-image limit |
| Events not firing | ID mismatch or element from .ui file (use `.editElement`) |

---

## Recommended Checklist

1. ✅ Put .ui and texture assets in `resources/Common/UI/Custom/`
2. ✅ Add `"IncludesAssetPack": true` to `manifest.json`
3. ✅ Image files must end with `@2x.png`
4. ✅ Run UI operations on world thread
5. ✅ Use HyUI for complex, interactive UIs
6. ✅ Use `.addEventListener` only for HYUIML/builder elements, not .ui file elements
7. ✅ Call `ctx.updatePage(true)` after modifying elements
8. ✅ Enable Diagnostic Mode for debugging

---

## References

- HyUI GitHub: https://github.com/Elliesaur/HyUI
- HyUI CurseForge: https://www.curseforge.com/hytale/mods/hyui
- Hytale UI Builder: https://hytale.ellie.au/
- Hytale Modding Docs: https://hytalemodding.dev/en/docs/guides/plugin/ui
- Full Living Reference: `.doc/references/ui-modding.md`
