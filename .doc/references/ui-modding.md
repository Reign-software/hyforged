# UI Modding Reference

This document provides comprehensive documentation for creating custom user interfaces in Hytale plugins using both raw `.ui` files and the **HyUI library**.

---

## Table of Contents

1. [Overview](#overview)
2. [Setup](#setup)
3. [HyUI Library](#hyui-library)
   - [Installation](#installation)
   - [PageBuilder](#pagebuilder---full-screen-pages)
   - [HudBuilder](#hudbuilder---persistent-huds)
   - [Detached Builders](#detached-builders)
   - [Event Handling](#event-handling)
   - [Dynamic Updates](#dynamic-updates)
4. [HYUIML Markup Language](#hyuiml-markup-language)
   - [Supported Tags](#supported-tags)
   - [Attributes Reference](#attributes-reference)
   - [CSS Styling](#css-styling)
   - [Layout Classes](#layout-classes)
   - [Tab Navigation](#tab-navigation)
   - [Image Handling](#image-handling)
5. [Template Processor](#template-processor)
   - [Variables](#variables)
   - [Loops](#loops)
   - [Conditionals](#conditionals)
   - [Components](#components)
6. [HyUI Builder Reference](#hyui-builder-reference)
7. [Raw .ui Files](#raw-ui-files)
8. [Thread Safety](#thread-safety)
9. [Troubleshooting](#troubleshooting)

---

## Overview

Hytale provides two paradigms for custom UI:

| Type | Description |
|------|-------------|
| **HUDs** | Always-visible overlays (health bars, minimaps, notifications) |
| **Pages** | Modal interfaces that capture focus (menus, dialogs, settings) |

### Approaches

| Approach | Best For |
|----------|----------|
| **HyUI + HYUIML** | Complex UIs, rapid development, multi-HUD systems |
| **HyUI + Builders** | Fine-grained control, dynamic UI construction |
| **HyUI + .ui Files** | Base layout from .ui, dynamic elements via builders |
| **Raw .ui Files** | Simple static UIs, learning Hytale UI basics |

---

## Setup

### Manifest Configuration

Your `manifest.json` must include:
```json
{
  "IncludesAssetPack": true
}
```

### File Structure

```
src/main/resources/
├── manifest.json
└── Common/
    └── UI/
        └── Custom/
            ├── Pages/         # .ui page files
            ├── Huds/          # .ui HUD files
            ├── MyTexture@2x.png  # Textures (must end with @2x.png)
            └── ...
```

### Debugging

Enable **Diagnostic Mode** in Hytale settings (under General) for detailed UI error messages.

---

## HyUI Library

HyUI is a fluent, builder-based library that simplifies Hytale UI development.

### Features

- **HYUIML**: HTML/CSS-like markup language
- **Fluent Builder API**: Clean, chainable Java API
- **Multi-HUD System**: Multiple independent HUDs without conflicts
- **Event Handling**: Lambda-based listeners with `UIContext`
- **Template Processor**: Variables, loops, conditionals, reusable components
- **Periodic Refresh**: Built-in batched HUD updates

### Installation

#### Maven

```xml
<repositories>
    <repository>
        <id>cursemaven</id>
        <name>CurseMaven</name>
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

#### Gradle

```gradle
repositories {
    maven { url "https://www.cursemaven.com" }
}

dependencies {
    // Project ID: 1431415 - get file-id from CurseForge files page
    implementation "curse.maven:hyui-1431415:<file-id>"
}
```

---

## PageBuilder - Full-Screen Pages

Pages are modal interfaces that block game input and show a cursor.

### Quick Start

```java
String html = """
    <div class="page-overlay">
        <div class="container" data-hyui-title="Welcome">
            <div class="container-contents">
                <p>Hello Hytale!</p>
                <button id="closeBtn">Close</button>
            </div>
        </div>
    </div>
    """;

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .addEventListener("closeBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
        ctx.getPage().ifPresent(HyUIPage::close);
    })
    .open(store);
```

### Content Sources

#### 1. HYUIML (HTML) - Recommended

```java
PageBuilder.pageForPlayer(playerRef)
    .fromHtml("""
        <div class="page-overlay">
            <div class="decorated-container" data-hyui-title="Settings">
                <div class="container-contents">
                    <label>Username:</label>
                    <input type="text" id="username" value="" placeholder="Enter name..." />
                    <button id="saveBtn">Save</button>
                </div>
            </div>
        </div>
    """)
    .addEventListener("saveBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
        ctx.getValue("username", String.class).ifPresent(name -> {
            playerRef.sendMessage(Message.raw("Saved: " + name));
        });
    })
    .open(store);
```

#### 2. From .ui Files

```java
PageBuilder.pageForPlayer(playerRef)
    .fromFile("Pages/MyPage.ui")
    .editElement(commands -> {
        commands.set("#Title.Text", "Dynamic Title");
    })
    .open(store);
```

> **Important**: Elements defined in `.ui` files cannot use `.addEventListener`. Use `.editElement` for raw UI commands.

#### 3. Manual Builder API

```java
PageBuilder.detachedPage()
    .withLifetime(CustomPageLifetime.CanDismiss)
    .addElement(PageOverlayBuilder.pageOverlay()
        .withId("overlay")
        .addChild(ContainerBuilder.container()
            .withTitleText("Built with Code")
            .addContentChild(LabelBuilder.label()
                .withText("This is a label"))
            .addContentChild(ButtonBuilder.textButton()
                .withId("myBtn")
                .withText("Click Me"))
        )
    )
    .addElement(ButtonBuilder.backButton())
    .addEventListener("myBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
        playerRef.sendMessage(Message.raw("Clicked!"));
    })
    .open(playerRef, store);
```

### Page Lifetime

```java
.withLifetime(CustomPageLifetime.CanDismiss)  // Standard dismissable
.withLifetime(CustomPageLifetime.Modal)       // Must close programmatically
```

### Modifying Base Elements (.ui file elements)

```java
PageBuilder.pageForPlayer(playerRef)
    .fromFile("Pages/Template.ui")
    .editElement(commands -> {
        commands.set("#Title.Text", "My Title");
        commands.set("#Description.Text", "My description");
        commands.set("#Button.Disabled", true);
    })
    .open(store);
```

---

## HudBuilder - Persistent HUDs

HUDs are always-visible overlays. HyUI automatically manages multiple HUDs.

### Quick Start

```java
HudBuilder.hudForPlayer(playerRef)
    .fromHtml("""
        <div style="anchor-top: 10; anchor-left: 10;">
            <p id="health">Health: 100</p>
        </div>
    """)
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
    .fromFile("Huds/StatusBar.ui")
    .show(store);

// Manual builders
HudBuilder.hudForPlayer(playerRef)
    .addElement(LabelBuilder.label()
        .withText("Manual HUD")
        .withAnchor(new HyUIAnchor().setTop(10).setLeft(10)))
    .show(store);
```

### Multi-HUD System

HyUI automatically manages Hytale's single HUD slot limitation:

- **Multiple independent HUDs** can run simultaneously (minimap, quest tracker, notifications)
- Each HUD has its **own refresh rate, elements, and event listeners**
- HUDs **don't interfere** with each other or other HyUI-based mods
- Just call `.show()` and HyUI handles composition

### Periodic Refreshing

```java
HudBuilder.hudForPlayer(playerRef)
    .withRefreshRate(1000) // Refresh every 1000ms (1 second)
    .onRefresh(hud -> {
        // Update elements on each refresh
        hud.getById("health", LabelBuilder.class).ifPresent(label -> {
            int health = getPlayerHealth(playerRef);
            label.withText("Health: " + health);
        });
        hud.getById("timer", LabelBuilder.class).ifPresent(label -> {
            label.withText("Time: " + formatTime(System.currentTimeMillis()));
        });
    })
    .show(store);
```

### Visibility and Lifecycle

```java
HyUIHud hud = HudBuilder.hudForPlayer(playerRef)
    .fromHtml("<p>My HUD</p>")
    .show(store);

// Control visibility
hud.hide();     // Hide but keep in multi-HUD manager
hud.unhide();   // Show again

// Remove/re-add
hud.remove();   // Completely remove from multi-HUD (stops refresh)
hud.readd();    // Re-add to multi-HUD manager
```

### Showing HUD on Player Join

```java
@EventHandler
public void onPlayerReady(PlayerReadyEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;
    
    Ref<EntityStore> ref = player.getReference();
    if (ref == null || !ref.isValid()) return;
    
    Store<EntityStore> store = ref.getStore();
    World world = store.getExternalData().getWorld();
    
    // MUST run on world thread
    world.execute(() -> {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;
        
        HudBuilder.detachedHud()
            .withId("welcome-hud")
            .fromHtml("""
                <div style="anchor-top: 10; anchor-right: 10;">
                    <p style="font-size: 18; font-weight: bold;">Welcome!</p>
                </div>
            """)
            .show(playerRef, store);
    });
}
```

---

## Detached Builders

Prepare UI configurations before you have a player reference:

```java
// Create detached configuration
PageBuilder pageConfig = PageBuilder.detachedPage()
    .fromHtml("""
        <div class="page-overlay">
            <p>Reusable Page</p>
        </div>
    """)
    .withLifetime(CustomPageLifetime.CanDismiss);

HudBuilder hudConfig = HudBuilder.detachedHud()
    .fromHtml("<p>Reusable HUD</p>")
    .withRefreshRate(1000);

// Later, open for specific players
pageConfig.open(playerRef1, store);
pageConfig.open(playerRef2, store);

hudConfig.show(playerRef1, store);
hudConfig.show(playerRef2, store);
```

---

## Event Handling

### Adding Event Listeners

```java
PageBuilder.pageForPlayer(playerRef)
    .fromHtml("""
        <button id="btn1">Button 1</button>
        <button id="btn2">Button 2</button>
        <input type="text" id="nameInput" value="" />
    """)
    .addEventListener("btn1", CustomUIEventBindingType.Activating, (data, ctx) -> {
        playerRef.sendMessage(Message.raw("Button 1 clicked!"));
    })
    .addEventListener("btn2", CustomUIEventBindingType.Activating, (data, ctx) -> {
        playerRef.sendMessage(Message.raw("Button 2 clicked!"));
    })
    .addEventListener("nameInput", CustomUIEventBindingType.ValueChanged, (data, ctx) -> {
        ctx.getValue("nameInput", String.class).ifPresent(value -> {
            playerRef.sendMessage(Message.raw("Input changed to: " + value));
        });
    })
    .open(store);
```

### UIContext Methods

The `UIContext` provides access to the current UI state:

```java
.addEventListener("myBtn", CustomUIEventBindingType.Activating, (data, ctx) -> {
    // Access the page
    ctx.getPage().ifPresent(page -> {
        page.close();
        page.reloadImage("dynamic-image-id");
    });
    
    // Get values from input elements
    Optional<String> text = ctx.getValue("textInput", String.class);
    Optional<Double> number = ctx.getValue("numberInput", Double.class);
    Optional<Boolean> checked = ctx.getValue("checkbox", Boolean.class);
    
    // Get builders for dynamic updates
    ctx.getById("label", LabelBuilder.class).ifPresent(label -> {
        label.withText("Updated text");
        ctx.updatePage(true); // Rebuild and send to client
    });
});
```

### Event Types

| Event Type | Trigger |
|------------|---------|
| `Activating` | Button click, enter key |
| `ValueChanged` | Input value changed |
| `ItemDragStart` | Item grid drag started |
| `ItemDragEnd` | Item grid drag ended |
| `ItemActivated` | Item slot activated |

---

## Dynamic Updates

### Updating Page Elements at Runtime

```java
AtomicInteger counter = new AtomicInteger(0);

PageBuilder.detachedPage()
    .fromHtml("""
        <p id="counter">Count: 0</p>
        <button id="increment">+1</button>
        <button id="reset">Reset</button>
    """)
    .addEventListener("increment", CustomUIEventBindingType.Activating, (data, ctx) -> {
        int newValue = counter.incrementAndGet();
        ctx.getById("counter", LabelBuilder.class).ifPresent(label -> {
            label.withText("Count: " + newValue);
            ctx.updatePage(true);
        });
    })
    .addEventListener("reset", CustomUIEventBindingType.Activating, (data, ctx) -> {
        counter.set(0);
        ctx.getById("counter", LabelBuilder.class).ifPresent(label -> {
            label.withText("Count: 0");
            ctx.updatePage(true);
        });
    })
    .open(playerRef, store);
```

> **Note**: When calling `ctx.updatePage(true)`, `Slider` elements may lose custom styles due to a Hytale issue.

### Reloading Dynamic Images

```java
.addEventListener("reload-btn", CustomUIEventBindingType.Activating, (data, ctx) -> {
    ctx.getValue("image-url", String.class).ifPresent(url -> {
        ctx.getById("my-image", DynamicImageBuilder.class).ifPresent(img -> {
            img.withImageUrl(url);
        });
        ctx.getPage().ifPresent(page -> page.reloadImage("my-image"));
    });
});
```

---

## HYUIML Markup Language

HYUIML is an HTML-like markup language that compiles to HyUI builders.

### Supported Tags

| HTML Tag | HyUI Builder | Notes |
|----------|--------------|-------|
| `<div>` | `GroupBuilder` | Layout containers |
| `<div class="page-overlay">` | `PageOverlayBuilder` | Full-screen overlay background |
| `<div class="container">` | `ContainerBuilder` | Hytale window frame |
| `<div class="decorated-container">` | `ContainerBuilder` | Styled decorated frame |
| `<div class="container-title">` | - | Content in container's title area |
| `<div class="container-contents">` | - | Content in container's main area |
| `<div class="tab-content">` | `TabContentBuilder` | Tab content panel |
| `<div class="item-grid">` | `ItemGridBuilder` | Scrollable item grid |
| `<div class="item-grid-slot">` | - | Slot inside item grid |
| `<p>` | `LabelBuilder` | Text paragraph |
| `<label>` | `LabelBuilder` | Text label (for forms) |
| `<button>` | `ButtonBuilder` | Standard button |
| `<button class="back-button">` | `ButtonBuilder` | Back/close button |
| `<button class="secondary-button">` | `ButtonBuilder` | Secondary style |
| `<button class="small-secondary-button">` | `ButtonBuilder` | Small secondary |
| `<button class="tertiary-button">` | `ButtonBuilder` | Tertiary style |
| `<button class="small-tertiary-button">` | `ButtonBuilder` | Small tertiary |
| `<input type="text">` | `TextFieldBuilder` | Text input |
| `<input type="password">` | `TextFieldBuilder` | Masked password input |
| `<input type="number">` | `NumberFieldBuilder` | Numeric input |
| `<input type="range">` | `SliderBuilder` | Slider control |
| `<input type="checkbox">` | `CheckBoxBuilder` | Toggle switch |
| `<input type="color">` | `ColorPickerBuilder` | Color picker |
| `<input type="reset">` | `ButtonBuilder` | Cancel/reset button |
| `<progress>` | `ProgressBarBuilder` | Horizontal progress bar |
| `<progress class="circular-progress">` | `ProgressBarBuilder` | Circular progress |
| `<img>` | `ImageBuilder` | Static image |
| `<img class="dynamic-image">` | `DynamicImageBuilder` | Runtime-downloaded PNG |
| `<hyvatar>` | `HyvatarImageBuilder` | Player avatar render |
| `<select>` | `DropdownBoxBuilder` | Dropdown list |
| `<option>` | - | Dropdown option (inside `<select>`) |
| `<nav class="tabs">` | `TabNavigationBuilder` | Tab navigation bar |
| `<sprite>` | `SpriteBuilder` | Animated sprite |
| `<span class="item-icon">` | `ItemIconBuilder` | Item icon display |
| `<span class="item-slot">` | `ItemSlotBuilder` | Full item slot |

---

## Attributes Reference

### Standard HTML Attributes

| Attribute | Elements | Description |
|-----------|----------|-------------|
| `id` | All | Element ID for events and `getById` |
| `class` | All | CSS classes for styling |
| `value` | Inputs | Initial value (required for event tracking) |
| `placeholder` | Text inputs | Placeholder hint text |
| `maxlength` | Text inputs | Maximum character count |
| `readonly` | Text inputs | Read-only mode (`true`/`false`) |
| `min`, `max`, `step` | Sliders, numbers | Range constraints |
| `checked` | Checkboxes | Initial state (`true`/`false`) |
| `format` | Number inputs | Number format string |
| `width`, `height` | Images | Dimensions (maps to anchor) |
| `src` | Images | Image source path or URL |

### HyUI Data Attributes (`data-hyui-*`)

| Attribute | Elements | Description |
|-----------|----------|-------------|
| `data-hyui-title` | Containers, overlays | Header title text |
| `data-hyui-tooltiptext` | Any | Tooltip on hover |
| `data-hyui-item-id` | Item icons/slots | In-game item ID |
| `data-hyui-show-quality-background` | Item slots | Show quality background |
| `data-hyui-show-quantity` | Item slots | Show quantity label |
| `data-hyui-style` | Any | Arbitrary style keys (e.g., `SlotSpacing: 6`) |

#### Progress Bar Attributes

| Attribute | Description |
|-----------|-------------|
| `data-hyui-bar-texture-path` | Fill texture path |
| `data-hyui-effect-texture-path` | Effect texture path |
| `data-hyui-effect-width` | Effect width |
| `data-hyui-effect-height` | Effect height |
| `data-hyui-effect-offset` | Effect offset |
| `data-hyui-direction` | Fill direction (`start`/`end`) |
| `data-hyui-alignment` | Orientation (`horizontal`/`vertical`) |
| `data-hyui-mask-texture-path` | Circular progress mask |
| `data-hyui-color` | Fill color (hex) |

#### Sprite Attributes

| Attribute | Description |
|-----------|-------------|
| `data-hyui-frame-width` | Frame width in pixels |
| `data-hyui-frame-height` | Frame height in pixels |
| `data-hyui-frame-per-row` | Frames per row in spritemap |
| `data-hyui-frame-count` | Total frame count |
| `data-hyui-fps` | Animation speed |

#### Item Grid Attributes

| Attribute | Description |
|-----------|-------------|
| `data-hyui-slots-per-row` | Slots per row |
| `data-hyui-background-mode` | Background mode |
| `data-hyui-render-item-quality-background` | Render quality backgrounds |
| `data-hyui-are-items-draggable` | Enable item dragging |
| `data-hyui-keep-scroll-position` | Maintain scroll position |
| `data-hyui-show-scrollbar` | Show scrollbar |

#### Tab Attributes

| Attribute | Description |
|-----------|-------------|
| `data-tabs` | Tab definitions: `id:Label,id2:Label2` |
| `data-selected` | Initially selected tab ID |
| `data-hyui-tab-id` | Link content to tab ID |
| `data-hyui-tab-nav` | Target specific nav (for multiple navs) |
| `data-tab` | On buttons: tab ID |
| `data-tab-content` | On buttons: linked content ID |

#### Dropdown Attributes

| Attribute | Description |
|-----------|-------------|
| `data-hyui-allowunselection` | Allow deselecting items |
| `data-hyui-maxselection` | Maximum selectable items |
| `data-hyui-entryheight` | Height of each entry |
| `data-hyui-showlabel` | Show/hide label |

#### Hyvatar Attributes

| Attribute | Description |
|-----------|-------------|
| `username` | Hyvatar username |
| `render` | `head`, `full`, or `cape` |
| `size` | Image size (64-2048) |
| `rotate` | Rotation angle (0-360) |
| `cape` | Cape override (for `render="cape"`) |

---

## CSS Styling

Include a `<style>` block in your HYUIML:

```html
<style>
    .title {
        color: #FFD700;
        font-size: 24;
        font-weight: bold;
    }
    
    #main-content {
        padding: 20;
        background-color: #1a1a1eE0;
    }
    
    .button-row {
        layout-mode: Left;
        padding-top: 10;
    }
</style>

<div class="page-overlay">
    <p class="title">Welcome!</p>
    <div id="main-content">
        <p>Content here</p>
        <div class="button-row">
            <button id="btn1">Button 1</button>
            <button id="btn2">Button 2</button>
        </div>
    </div>
</div>
```

### Supported CSS Properties

| Property | Values | Notes |
|----------|--------|-------|
| `color` | `#RRGGBB`, `#RRGGBBAA` | Text color |
| `font-size` | Number | Font size in pixels |
| `font-weight` | `bold`, `normal` | Font weight |
| `text-transform` | `uppercase`, `none` | Text transformation |
| `text-align` | `left`, `right`, `center`, `top`, `bottom`, etc. | For `<div>`, maps to LayoutMode |
| `vertical-align` | `top`, `bottom`, `center` | Vertical alignment |
| `horizontal-align` | `left`, `right`, `center` | Horizontal alignment |
| `align` | `center`, etc. | Combined alignment |
| `visibility` | `hidden`, `shown` | Element visibility |
| `display` | `none`, `block` | Alternative to visibility |
| `layout-mode`, `layout` | `Top`, `Left`, `Center`, etc. | Group layout mode |
| `flex-weight` | Number | Layout weight |
| `flex-direction` | `row`, `column` | Partial flexbox support |
| `align-items` | `center`, etc. | Partial flexbox support |
| `justify-content` | `center`, etc. | Partial flexbox support |
| `padding` | Number | All sides |
| `padding-left`, `padding-right`, `padding-top`, `padding-bottom` | Number | Individual sides |
| `anchor-left`, `anchor-right`, `anchor-top`, `anchor-bottom` | Number | Positioning |
| `anchor-width`, `anchor-height` | Number | Size |
| `background-color` | `#RRGGBB [border]` | Background with optional border |
| `background-image` | `url('path.png') [h-border] [v-border]` | Background texture |
| `hyui-style-reference` | `"File.ui" "StyleName"` | Reference style from .ui file |

### Style Reference Properties

For advanced styling, reference styles from .ui files:

```css
#myDropdown {
    hyui-entry-label-style: "Common.ui" "DefaultLabelStyle";
    hyui-selected-entry-label-style: "Common.ui" "SelectedLabelStyle";
    hyui-popup-style: "Common.ui" "DefaultPopupStyle";
    hyui-number-field-style: "Common.ui" "NumberFieldStyle";
    hyui-checked-style: "Common.ui" "CheckedStyle";
    hyui-unchecked-style: "Common.ui" "UncheckedStyle";
}
```

### Custom Style Properties

Use `data-hyui-style` for arbitrary style keys:

```html
<div class="item-grid" data-hyui-style="SlotSpacing: 6; SlotSize: 64"></div>
```

---

## Layout Classes

### Page Structure

```html
<div class="page-overlay">
    <div class="container" data-hyui-title="My Page">
        <div class="container-title">
            <!-- Elements in title bar area -->
            <button id="help">?</button>
        </div>
        <div class="container-contents">
            <!-- Main content area -->
            <p>Page content here</p>
        </div>
    </div>
</div>
```

### Container Variants

| Class | Description |
|-------|-------------|
| `.page-overlay` | Full-screen background overlay |
| `.container` | Standard Hytale window frame |
| `.decorated-container` | Styled decorated frame |
| `.container-title` | Title bar area (child of container) |
| `.container-contents` | Main content area (child of container) |

---

## Tab Navigation

### Using `data-tabs`

```html
<nav id="main-tabs" class="tabs"
     data-tabs="general:General,settings:Settings,about:About"
     data-selected="general">
</nav>

<div class="tab-content" data-hyui-tab-id="general">
    <p>General tab content</p>
</div>

<div class="tab-content" data-hyui-tab-id="settings">
    <p>Settings tab content</p>
</div>

<div class="tab-content" data-hyui-tab-id="about">
    <p>About tab content</p>
</div>
```

### Linking Content Directly

```html
<nav class="tabs"
     data-tabs="tab1:Tab One:tab1-content,tab2:Tab Two:tab2-content"
     data-selected="tab1">
</nav>
```

### Using Explicit Buttons

```html
<nav class="tabs" data-selected="general">
    <button data-tab="general" data-tab-content="general-content">General</button>
    <button data-tab="settings" data-tab-content="settings-content">Settings</button>
</nav>
```

### Multiple Tab Navigations

```html
<nav id="left-nav" class="tabs" data-tabs="a:A,b:B" data-selected="a"></nav>
<nav id="right-nav" class="tabs" data-tabs="x:X,y:Y" data-selected="x"></nav>

<div class="tab-content" data-hyui-tab-id="a" data-hyui-tab-nav="left-nav">
    <p>Left Nav Tab A</p>
</div>

<div class="tab-content" data-hyui-tab-id="x" data-hyui-tab-nav="right-nav">
    <p>Right Nav Tab X</p>
</div>
```

---

## Image Handling

### Static Images

Images are relative to `Common/UI/Custom/`. **Files must end with `@2x.png`**:

```html
<!-- Usage -->
<img src="icons/sword.png" style="anchor-width: 32; anchor-height: 32;" />

<!-- File location -->
src/main/resources/Common/UI/Custom/icons/sword@2x.png
```

### Background Images

```css
.my-panel {
    background-image: url('panel.png');
    background-image: url('panel.png') 4;      /* With border */
    background-image: url('panel.png') 4 6;    /* Horizontal, vertical border */
}
```

### Dynamic Images

Download PNGs at runtime:

```html
<img class="dynamic-image" src="https://example.com/avatar.png" 
     style="anchor-width: 64; anchor-height: 64;" />
```

**Limits:**
- 10 dynamic images per page per player
- Images cached for 15 seconds

### Hyvatar Integration

Render player avatars via Hyvatar.io:

```html
<hyvatar username="Elyra" render="head" size="256"></hyvatar>
<hyvatar username="Elyra" render="full" size="512" rotate="45"></hyvatar>
<hyvatar username="Elyra" render="cape" cape="MyCape"></hyvatar>
```

| Attribute | Values | Description |
|-----------|--------|-------------|
| `username` | String | Hyvatar username |
| `render` | `head`, `full`, `cape` | Render type |
| `size` | 64-2048 | Image size in pixels |
| `rotate` | 0-360 | Rotation angle in degrees |
| `cape` | String | Cape override for `render="cape"` |

---

## Template Processor

For data-driven UIs with variables, loops, and reusable components.

### Variables

```java
TemplateProcessor template = new TemplateProcessor()
    .setVariable("playerName", playerRef.getUsername())
    .setVariable("playerLevel", 42)
    .setVariable("gold", 1500);
```

```html
<p>Welcome, {{$playerName}}!</p>
<p>Level: {{$playerLevel}}</p>
<p>Gold: {{$gold|number}}</p>

<!-- Default values -->
<p>Guild: {{$guild|None}}</p>

<!-- Filters -->
<p>{{$playerName|upper}}</p>

<!-- Nested paths -->
<p>Tier: {{$meta.tier}}</p>
<p>First item: {{$items.0.name}}</p>
```

### Loops

```java
List<Item> items = getPlayerItems();
template.setVariable("items", items);
```

```html
{{#each items}}
    <div class="item-row">
        <span class="item-icon" data-hyui-item-id="{{$id}}"></span>
        <p>{{$name}} x{{$quantity}}</p>
    </div>
{{/each}}
```

### Conditionals

```html
{{#if isAdmin}}
    <button id="admin-panel">Admin Panel</button>
{{else}}
    <p>Standard user</p>
{{/if}}

{{#if level >= 10 && hasUnlockedSkills}}
    <button id="skills">Skills</button>
{{/if}}

{{#if tags contains "rare" || rarity == Epic}}
    <p style="color: #FFD700;">Rare Item!</p>
{{/if}}
```

**Supported Operators:**
- Equality: `==`, `!=`
- Numeric: `>`, `<`, `>=`, `<=`
- Logical: `&&`, `||`, `!`
- Contains: `contains` (strings, arrays, iterables, map keys)

### Components (Reusable Blocks)

```java
template.registerComponent("statCard", """
    <div style="background-color: #2a2a3e; padding: 10; anchor-width: 120;">
        <p style="color: #888; font-size: 11;">{{$label}}</p>
        <p style="color: #fff; font-size: 18; font-weight: bold;">{{$value}}</p>
    </div>
""");

template.registerComponent("itemRow", """
    <div style="layout-mode: Left; padding: 4;">
        <span class="item-icon" data-hyui-item-id="{{$itemId}}" 
              style="anchor-width: 32; anchor-height: 32;"></span>
        <p style="flex-weight: 1; padding-left: 8;">{{$name}}</p>
        <p style="color: #888;">x{{$count}}</p>
    </div>
""");
```

```html
<div style="layout-mode: Left;">
    {{@statCard:label=Health,value=100}}
    {{@statCard:label=Mana,value=50}}
    {{@statCard:label=Stamina,value=75}}
</div>

<div style="layout-mode: Top;">
    {{@itemRow:itemId=Tool_Pickaxe_Crude,name=Crude Pickaxe,count=1}}
    {{@itemRow:itemId=Material_Wood,name=Wood,count=64}}
</div>
```

### Using Components with Loops

```java
template.setVariable("inventory", playerInventory)
    .registerComponent("inventorySlot", """
        <div class="slot" style="padding: 4;">
            {{#if quantity > 0}}
                <span class="item-icon" data-hyui-item-id="{{$itemId}}"></span>
                <p>{{$name}} x{{$quantity}}</p>
            {{else}}
                <p style="color: #666;">Empty</p>
            {{/if}}
        </div>
    """);
```

```html
{{#each inventory}}
    {{@inventorySlot:itemId={{$id}},name={{$name}},quantity={{$count}}}}
{{/each}}
```

### Complete Example

```java
TemplateProcessor template = new TemplateProcessor()
    .setVariable("playerName", playerRef.getUsername())
    .setVariable("level", 42)
    .setVariable("stats", Map.of("health", 100, "mana", 50, "stamina", 75))
    .setVariable("inventory", playerItems)
    .registerComponent("statBar", """
        <div style="layout-mode: Left; padding: 4;">
            <p style="anchor-width: 80;">{{$label}}:</p>
            <progress value="{{$percent}}" 
                      style="anchor-width: 100; anchor-height: 16;"
                      data-hyui-color="{{$color}}"></progress>
            <p style="padding-left: 8;">{{$value}}/{{$max}}</p>
        </div>
    """);

String html = template.process("""
    <div class="page-overlay">
        <div class="decorated-container" data-hyui-title="{{$playerName}} - Level {{$level}}">
            <div class="container-contents">
                {{@statBar:label=Health,value=85,max=100,percent=0.85,color=#ff4444}}
                {{@statBar:label=Mana,value=40,max=50,percent=0.80,color=#4444ff}}
                
                <p style="font-weight: bold; padding-top: 10;">Inventory:</p>
                {{#each inventory}}
                    <p>{{$name}} x{{$count}}</p>
                {{/each}}
            </div>
        </div>
    </div>
""");

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .open(store);
```

---

## HyUI Builder Reference

### Page/HUD Builders

| Builder | Purpose | Key Methods |
|---------|---------|-------------|
| `PageBuilder` | Full-screen pages | `fromHtml()`, `fromFile()`, `addElement()`, `addEventListener()`, `open()` |
| `HudBuilder` | Persistent HUDs | `fromHtml()`, `fromFile()`, `addElement()`, `withRefreshRate()`, `show()` |

### Container Builders

| Builder | Purpose | Key Methods |
|---------|---------|-------------|
| `GroupBuilder` | Layout container | `withLayoutMode()`, `addChild()`, `withAnchor()` |
| `PageOverlayBuilder` | Full-screen overlay | `addChild()` |
| `ContainerBuilder` | Window frame | `withTitleText()`, `addContentChild()`, `addTitleChild()` |
| `TabNavigationBuilder` | Tab bar | `addTab()`, `withSelectedTab()` |
| `TabContentBuilder` | Tab content | `withTabId()`, `addChild()` |

### Input Builders

| Builder | Purpose | Key Methods |
|---------|---------|-------------|
| `TextFieldBuilder` | Text input | `withValue()`, `withPlaceholder()`, `withMaxLength()` |
| `NumberFieldBuilder` | Number input | `withValue()`, `withFormat()` |
| `SliderBuilder` | Range slider | `withMin()`, `withMax()`, `withStep()`, `withValue()` |
| `CheckBoxBuilder` | Toggle | `withChecked()` |
| `ColorPickerBuilder` | Color picker | `withValue()` |
| `DropdownBoxBuilder` | Dropdown | `addEntry()`, `withValue()`, `withEntries()` |

### Display Builders

| Builder | Purpose | Key Methods |
|---------|---------|-------------|
| `LabelBuilder` | Text | `withText()`, `withStyle()` |
| `ButtonBuilder` | Buttons | `withText()`, `textButton()`, `backButton()` |
| `ImageBuilder` | Static images | `withSource()`, `withAnchor()` |
| `DynamicImageBuilder` | Downloaded images | `withImageUrl()` |
| `HyvatarImageBuilder` | Player avatars | `withUsername()`, `withRenderType()`, `withSize()` |
| `ProgressBarBuilder` | Progress bars | `withValue()`, `circularProgressBar()` |
| `SpriteBuilder` | Animated sprites | `withTexture()`, `withFrame()`, `withFramesPerSecond()` |
| `ItemIconBuilder` | Item icons | `withItemId()` |
| `ItemSlotBuilder` | Item slots | `withItemId()`, `withQuantity()` |
| `ItemGridBuilder` | Item grids | `addSlot()`, `withSlotsPerRow()` |
| `TimerLabelBuilder` | Timer display | `withFormat()` |

### Common Methods

```java
// All builders support:
.withId("element-id")                    // ID for events/getById
.withAnchor(new HyUIAnchor())            // Position and size
.withStyle(new HyUIStyle())              // Styling
.withVisible(true/false)                 // Visibility
.addChild(childBuilder)                  // Add child element
.addEventListener(type, handler)          // Event listener
```

### HyUIAnchor

```java
new HyUIAnchor()
    .setLeft(10)
    .setTop(10)
    .setWidth(200)
    .setHeight(50)
    .setRight(10)
    .setBottom(10)
    .setHorizontal(0)  // Center horizontally
    .setVertical(0)    // Center vertically
    .setFull(0)        // Fill parent
```

### HyUIStyle

```java
new HyUIStyle()
    .setFontSize(14)
    .setRenderBold(true)
    .setAlignment("Center")
    .setColor("#FFD700")
```

---

## Raw .ui Files

For simple UIs or when HyUI is unavailable.

### Location

```
src/main/resources/Common/UI/Custom/
```

### Syntax

```ui
// Comment
$Common = "../Common.ui";

Group #Root {
    Anchor: (Full: 0);
    LayoutMode: Top;
    Background: PatchStyle(Color: #1a1a1e);
    Padding: (Full: 10);
    
    Label #Title {
        Text: "Hello World";
        Style: (FontSize: 24, RenderBold: true);
    }
    
    Group #ButtonRow {
        LayoutMode: Left;
        Anchor: (Height: 40);
        
        Button #SaveBtn {
            Content: "Save";
        }
        
        Button #CancelBtn {
            Content: "Cancel";
        }
    }
    
    TextField #NameInput {
        Anchor: (Width: 200, Height: 30);
        PlaceholderText: "Enter name...";
    }
}
```

### Elements

| Element | Description |
|---------|-------------|
| `Group` | Container (like div) |
| `Label` | Text display |
| `Button` | Clickable button |
| `TextField` | Text input |
| `Timer` | Timer display |
| `ColorPicker` | Color selector |

### Properties

| Property | Example |
|----------|---------|
| `Anchor` | `(Left: 10, Top: 10, Width: 200, Height: 50)` |
| `LayoutMode` | `Top`, `Left`, `Center` |
| `Style` | `(FontSize: 14, RenderBold: true, Alignment: Center)` |
| `Background` | `PatchStyle(Color: #1a1a1e)` |
| `Padding` | `(Full: 10)`, `(Left: 5, Right: 5)` |
| `Text` | `"Hello World"` |
| `Content` | `"Button Text"` (for buttons) |
| `PlaceholderText` | `"Hint text..."` |
| `Visibility` | `Collapsed`, `Visible` |
| `ClipToBounds` | `true`, `false` |
| `FlexWeight` | `1` |

### Variables

```ui
@MyBackground = PatchStyle(TexturePath: "bg.png");
@TitleStyle = (FontSize: 24, RenderBold: true);

Group {
    Background: @MyBackground;
    
    Label {
        Style: @TitleStyle;
        Text: "Title";
    }
}
```

### Including Files

```ui
$Common = "../Common.ui";

Group {
    Background: $Common.@InputBoxBackground;
    Style: $Common.@DefaultLabelStyle;
}
```

---

## Thread Safety

**UI operations MUST run on the world thread** or the game will crash.

### For Commands

```java
public class MyCommand extends AbstractAsyncCommand {
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        if (context.sender() instanceof Player player) {
            Ref<EntityStore> ref = player.getReference();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);
            
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            
            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef != null) {
                    PageBuilder.pageForPlayer(playerRef)
                        .fromHtml("<p>Hello!</p>")
                        .open(store);
                }
            }, world);
        }
        return CompletableFuture.completedFuture(null);
    }
}
```

### For HUDs

```java
world.execute(() -> {
    HudBuilder.hudForPlayer(playerRef)
        .fromHtml("<p>HUD Content</p>")
        .show(store);
});
```

### For Event Handlers

Event handlers within HyUI pages/HUDs already run on the correct thread — no special handling needed.

---

## Troubleshooting

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `Failed to apply Custom UI HUD commands` | Syntax error in .ui file | Enable Diagnostic Mode, check syntax |
| `Could not find document for Custom UI Append` | Wrong path | Ensure file is in `Common/UI/Custom/` |
| `Unknown node type: X` | Invalid element type | Check supported elements (no `ScrollViewer`, etc.) |
| `Expected end of file` | XML wrapper or HTML comments | Use `//` comments, no XML wrappers |
| Client disconnect on UI open | Not on world thread | Use `world.execute()` |
| Dynamic images not loading | URL or limit issue | Check URL, max 10 per page |
| Events not firing | ID mismatch or .ui element | Use `getById` with original ID, or `.editElement` for .ui files |
| Slider loses style on update | Hytale bug | Known issue with `updatePage(true)` |

### Debugging Tips

1. **Enable Diagnostic Mode** in Hytale settings (General tab)
2. **Check file locations** — all custom UI in `Common/UI/Custom/`
3. **Image naming** — files must end with `@2x.png`
4. **Thread safety** — always use `world.execute()` for UI operations outside event handlers
5. **IDs** — HyUI sanitizes IDs internally; always use your original ID in Java
6. **Check server logs** — UI errors often logged server-side

### Important Limitations

1. **No ScrollViewer** — Hytale doesn't support this element
2. **Limited CSS** — Only documented properties work; no full flexbox/grid
3. **No scripting** — `<script>` tags are ignored; all logic in Java
4. **ID sanitization** — Use original IDs in Java, HyUI handles internal sanitization
5. **.ui file events** — Use `.editElement`, not `.addEventListener`

---

## References

- **HyUI GitHub**: https://github.com/Elliesaur/HyUI
- **HyUI CurseForge**: https://www.curseforge.com/hytale/mods/hyui
- **HyUI Discord**: https://discord.gg/NYeK9JqmNB
- **Hytale UI Builder**: https://hytale.ellie.au/
- **Hytale Modding Docs**: https://hytalemodding.dev/en/docs/guides/plugin/ui
- **Example Project**: https://github.com/Elliesaur/Hytale-Example-UI-Project
