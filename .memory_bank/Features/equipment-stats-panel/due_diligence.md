# Due Diligence: Equipment Panel on Character Stats Page

## API Availability

### Available APIs

- **`ItemIcon` UI element** — Exists in the Hytale UI type system (`references/type-documentation.md`). Properties: `ItemId`, `Quantity`. However, only one actual usage found in the entire `lib/UI/` codebase: `ItemQuantityPopup.ui` uses an `ItemGrid` with `SlotsPerRow: 1` as a single-item icon display — not `ItemIcon` directly. This suggests `ItemIcon` may work but `ItemGrid` is the battle-tested pattern for displaying item visuals.
- **`ItemGrid` UI element** — Well-established for displaying items with their icons. Supports `Slots` (set via `ItemGridSlot[]`), `SlotsPerRow`, `RenderItemQualityBackground`, `InfoDisplay`, and `SlotSize` styling. The `ItemQuantityPopup.ui` pattern of a 1-slot ItemGrid is the closest existing example of a single-item icon display.
- **`ItemGridSlot`** — Server-side Java class (`com.hypixel.hytale.server.core.ui.ItemGridSlot`) with `.setItemStack(ItemStack)` for populating grid slots from Java. Supports custom name, description, background, overlay, and `SkipItemQualityBackground`.
- **`UICommandBuilder.setObject()`** — Confirmed to accept `ItemGridSlot[]` for setting grid slots: `commands.setObject("#Grid.Slots", new ItemGridSlot[]{ new ItemGridSlot(itemStack) })`.
- **`UICommandBuilder.appendInline()`** — Confirmed working in `InteractiveCustomUIPage.build()` context. Used extensively by `PassiveTreePage.java` and `ConcentrationPriorityPage.java` during `build()`.
- **`InteractiveCustomUIPage`** — Full event binding support confirmed. `CharacterStatsPage.java` already uses `MouseEntered`/`MouseExited` for modifier tooltip. `PassiveTreePage.java` binds 3 events per node (click + hover enter + hover exit) for potentially 50+ nodes without issues.
- **`ItemQuality.getTextColor()`** — Returns `com.hypixel.hytale.protocol.Color` with `.red`, `.green`, `.blue` byte fields. Available via `ItemQuality.getAssetMap().getAsset(qualityId)`.
- **`HyforgedQualityService.getEffectiveQuality()`** — Returns quality ID string from ItemStack metadata or falls back to item's base quality.
- **`AffixTooltipProvider.generateTooltip(HyforgedItemData)`** — Returns `TooltipContent` with data-driven sections. Already used by `HyforgedHud.updateItemAffixes()` for weapon-swap tooltip.
- **`HyforgedItemDataService.read(ItemStack)`** — Returns `HyforgedItemData` from item metadata.
- **`Player.getInventory().getArmor()`** and **`getHotbar()`** — Already used in `CharacterStatsPage.buildEquipmentSummary()`.
- **`CustomUIEventBindingType.MouseEntered` / `MouseExited`** — Available for hover detection. Already used in CharacterStatsPage for modifier tooltips.
- **`CustomUIEventBindingType.SlotMouseEntered` / `SlotMouseExited`** — Available specifically for `ItemGrid` slot hover events. These are the correct events for detecting hover on individual ItemGrid slots.
- **`Message.translation()` / `Message.raw()` / `.color()` / `.insert()`** — Full rich text API available for tooltip building.

### Missing or Uncertain APIs

- **`ItemIcon` standalone element** — While listed in type documentation, there is no example of it being used as a standalone display element populated from server-side Java. The `ItemId` property suggests it takes a string item ID, but it's unclear whether `UICommandBuilder.set("#MyItemIcon.ItemId", "hytale:iron_sword")` would work correctly. **Risk: Medium.** Mitigation: Use the proven `ItemGrid` single-slot pattern instead.
- **Dynamic tooltip positioning** — No API discovered for querying an element's rendered position from server-side Java. The current modifier tooltip uses a fixed `Anchor: (Left: 350, Top: 78)` position. Equipment tooltips will need fixed positioning or a reasonable static offset. **Risk: Low.** Mitigation: Position equipment tooltip panel at a known fixed location relative to the equipment panel, just as the modifier tooltip does.
- **`Color` to hex string conversion** — No built-in utility, but `AffixTooltipProvider.toHexColor(Color)` already implements this pattern. It's private, so either duplicate or extract to a shared utility. Multiple other classes (`XPAwardSystem`, `PlayerDeathCombatLogSystem`) use the same inline pattern.

## Existing Code Impact

### Files to Modify

- **`src/main/java/reign/software/hyforged/affix/ui/CharacterStatsPage.java`** (977 lines)
  - Replace `buildEquipmentSummary()` (currently at lines ~430-490) with new equipment panel builder using ItemGrid slots + hover events
  - Add equipment tooltip show/hide handlers (pattern from `handleShowModifierTooltip`)
  - Remove range column population from `buildCategoryGroup()` (line ~370: `commandBuilder.set(rowSelector + " #RangeValue.TextSpans", ...)`)
  - Remove `#ColRange` header population from `build()` (add range info to stat description tooltip)
  - Enhance `buildStatDescriptionTooltip()` to append range info
  - Add `PageEventData` codec fields for equipment tooltip target (or reuse existing `TooltipTarget` key with a new action)
  - Add `formatRange()` call within `buildStatDescriptionTooltip()` instead of as a separate column

- **`src/main/resources/Common/UI/Custom/Hyforged/CharacterStatsPage.ui`**
  - Widen `#CharacterStatsPage` from `Width: 900` to ~`1120`
  - Restructure layout to add a left-side equipment panel container
  - Remove `#ColRange` from `#ColumnHeaders`
  - Remove `#EquipmentColumn` group (old text-based equipment section)
  - Add `#EquipmentPanel` group on left side with 6 ItemGrid elements (one per slot)
  - Add `#EquipmentTooltip` overlay (similar to `#ModifierTooltip`)
  - Shift `#Content` and `#ColumnHeaders` to accommodate left panel offset

- **`src/main/resources/Common/UI/Custom/Hyforged/CharacterStatRow.ui`**
  - Remove `#RangeValue` Label element (currently `Anchor: (Width: 140)`)

- **`src/main/resources/Server/Languages/en-US/hyforged.lang`**
  - Add slot label keys: `characterStats.slot.head`, `characterStats.slot.chest`, `characterStats.slot.legs`, `characterStats.slot.feet`, `characterStats.slot.mainHand`, `characterStats.slot.offHand`
  - Update `characterStats.slot.empty` (currently has `Slot {slot}: Empty`, may become just `Empty`)
  - Add `characterStats.range` key for range label in tooltip (e.g., `Range:`)

### Files to Create

- **`src/main/resources/Common/UI/Custom/Hyforged/EquipmentSlot.ui`** (optional) — Template for a single equipment slot containing an ItemGrid, slot label, and slot background. Could also be built inline via `appendInline()` to keep it simpler.

### Related TODOs Found

- No TODOs found in the codebase related to equipment panel, range column, or the stats page layout changes.

## Integration Points

- **AffixTooltipProvider** — Used to generate affix tooltip sections for equipped items. Call `generateTooltip(HyforgedItemDataService.read(itemStack))` per slot item. Already stable and tested.
- **HyforgedQualityService** — Used to resolve quality ID from ItemStack. Then lookup `ItemQuality.getAssetMap().getAsset(qualityId)` to get `getTextColor()`. Multiple existing patterns in codebase (`XPAwardSystem`, `PlayerDeathCombatLogSystem`, `AffixTooltipProvider.buildQualityLines()`).
- **HyforgedItemDataService** — Used to read affix data from equipped items. Simple `read(itemStack)` call.
- **StatDefinition** — The `formatRange()` method already exists in `CharacterStatsPage` at line ~875. Will be reused for the enhanced stat description tooltip. `StatDefinition` provides `minValue()`, `maxValue()`, `description()`, `displayFormat()`.
- **Modifier tooltip pattern** — The existing `#ModifierTooltip` overlay, `handleShowModifierTooltip()`/`handleHideModifierTooltip()`, and `modifierTooltipByRowSelector` map provide a direct template for the equipment tooltip. Same pattern: fixed-position overlay, show/hide via `MouseEntered`/`MouseExited`, text updated via `sendUpdate()`.
- **PassiveTreePage event pattern** — Confirms large numbers of event bindings work (3 per node x 50+ nodes = 150+ bindings). Adding 12 equipment events (6 MouseEntered + 6 MouseExited) is trivial.
- **Weapon-swap HUD affix display** — `HyforgedHud.updateItemAffixes()` uses `appendInline()` to dynamically create Labels for section headers and affix lines with per-line colors. This pattern can be adapted for the page-context equipment tooltip, but since the tooltip is smaller (single item), pre-building the `Message` text may be simpler than dynamic element creation.

## Dependencies

- No new external dependencies required.
- No plugin ordering changes needed.
- No manifest changes needed (already has `IncludesAssetPack: true`).

## Data Architecture

### New Components

- None. This is purely a UI change reading existing data from player inventory, `HyforgedItemData`, and `HyforgedQualityService`.

### New JSON Definitions

- None. All data is already available through existing systems.

### Serialization

- **`PageEventData` codec expansion** — The existing codec already has `Action` and `TooltipTarget` keys. Equipment tooltip can reuse these same keys with new action values (e.g., `"showEquipmentTooltip"`, `"hideEquipmentTooltip"`) and a target identifier (e.g., `"armor:0"`, `"hand:1"`). No new codec fields needed unless a slot index is passed separately.

## Risk Assessment

### High Risk

- None identified.

### Medium Risk

- **ItemIcon / ItemGrid rendering in custom InteractiveCustomUIPage** — While ItemGrid + ItemGridSlot is confirmed to work through the API (`setObject` with `ItemGridSlot[]`), it has not been tested specifically in the Hyforged codebase. The inventory system uses ItemGrid extensively in Hytale's built-in pages, but custom pages have not been confirmed to render item icons correctly. **Impact**: If item icon rendering fails, the entire visual panel would need to fall back to text-only. **Mitigation**: Implement ItemGrid approach first; have text-based fallback ready. A quick spike test (single ItemGrid with one slot) should be done before committing to the full implementation.

- **Page width > 1000px** — Currently 900px. Widening to ~1120px may clip on smaller resolutions or look awkward if Hytale has internal window size constraints. **Impact**: UI may not fit on some screens. **Mitigation**: Test at minimum supported resolution; consider that 1120px is still well under 1920px standard. The built-in inventory pages are larger.

### Low Risk

- **Event binding count** — Adding 12 equipment bindings on top of existing modifier tooltip bindings (potentially 30+ for stats) and footer bindings (3). PassiveTreePage handles 150+ event bindings without issue, so ~45+ total is well within limits. **Mitigation**: None needed.

- **`appendInline()` in InteractiveCustomUIPage** — Confirmed working in `PassiveTreePage.build()` and `ConcentrationPriorityPage.build()`. No risk.

- **Quality color resolution** — Pattern is well-established across 4+ existing callsites. `ItemQuality.getAssetMap().getAsset(qualityId).getTextColor()` is reliable. Null-safe wrappers exist. **Mitigation**: Use try-catch with fallback color (`#CCCCCC`), matching existing patterns.

- **Tooltip content computation cost** — `AffixTooltipProvider.generateTooltip()` for 6 items at page open. Each call is fast (simple list iteration). Total overhead well under 5ms. **Mitigation**: None needed; compute eagerly during `build()`.

- **Column width redistribution** — Removing `#RangeValue` (140px) from `.ui` template and `#ColRange` from headers is mechanical. Remaining columns: Stat(270) + Base(110) + Modifier(130) + Effective(110) = 620px within a wider stats content area. Can redistribute ~140px to Stat name column or add padding. **Mitigation**: Visual testing.

## Unknowns & Blockers

- **ItemGrid slot hover events in custom pages** — While `SlotMouseEntered` / `SlotMouseExited` exist as `CustomUIEventBindingType` values, they have only been confirmed on built-in inventory pages. Using 6 separate 1-slot ItemGrids (one per equipment slot) with `MouseEntered`/`MouseExited` on the containing Group avoids this concern entirely, and is the recommended approach.
- **ItemIcon standalone element** — Unknown if `ItemIcon { ItemId: "hytale:iron_sword"; }` can be dynamically populated from server-side Java. The ItemGrid approach bypasses this question.
- **Hytale internal screen-space constraints** — Unknown if there's a maximum page width enforced by the client. 1120px should be safe but is untested.

## Recommendations

### Technical Approach

1. **Equipment panel: Use 6 separate 1-slot `ItemGrid` elements**, one per equipment slot. Each ItemGrid is a child of a named Group (`#Slot0`, `#Slot1`, etc.) that also contains a Label for the slot name. Populate via `commandBuilder.setObject("#Slot0Grid.Slots", new ItemGridSlot[]{ new ItemGridSlot(itemStack) })`. This avoids the uncertainty of `ItemIcon` and `SlotMouseEntered/Exited` on a multi-slot grid, while giving proper item icon rendering with quality background.

2. **Equipment tooltip: Reuse the modifier tooltip pattern.** Add a second overlay `#EquipmentTooltip` in the `.ui` file, positioned on the left side near the equipment panel (e.g., `Anchor: (Left: 10, Top: 78, Width: 240, Height: auto)`). Use `Message.raw()` with `.color()` and `.insert()` to build rich tooltip text with item name (quality-colored), quality label, and affix lines. Show/hide via `sendUpdate()` on `MouseEntered`/`MouseExited` events bound to each slot's container Group.

3. **Text fallback**: If ItemGrid slot rendering proves problematic (spike test), fall back to Label elements with quality-colored item name text. The tooltip pattern remains identical — only the slot visual changes.

4. **Range column removal**: Straightforward mechanical change. Remove `#ColRange` from `.ui`, remove `#RangeValue` from `CharacterStatRow.ui`, remove the `commandBuilder.set(rowSelector + " #RangeValue.TextSpans", ...)` line from `buildCategoryGroup()`. Add range info to `buildStatDescriptionTooltip()` by calling the existing `formatRange()` method.

5. **Quality color utility**: Extract the Color-to-hex-string pattern into a shared utility method (e.g., in `MessageColors` or a new `ColorUtil`), or keep it inline in `CharacterStatsPage` to avoid scope creep. Multiple existing callsites use the same 3-line pattern.

6. **PageEventData codec**: Reuse existing `Action` + `TooltipTarget` keys. New action values: `"showEquipTooltip"` / `"hideEquipTooltip"`. `TooltipTarget` carries the slot identifier (e.g., `"armor:0"`, `"hand:1"`). Pre-build tooltip `Message` objects per slot during `build()` into a `Map<String, Message>`, mirroring the existing `modifierTooltipByRowSelector` pattern.

### Phasing

Given the moderate scope, this can be implemented in a single phase with these steps:

1. **Step 1**: Spike test — Create a minimal ItemGrid in the character stats page to confirm item icon rendering works in custom pages.
2. **Step 2**: Layout changes — Widen page, add equipment panel container, remove Range column from `.ui` files.
3. **Step 3**: Equipment panel population — Build 6 slot groups with ItemGrid + labels, populate from inventory.
4. **Step 4**: Equipment tooltip — Add overlay, event bindings, tooltip content generation.
5. **Step 5**: Range removal from Java — Remove range column logic, enhance stat description tooltip.
6. **Step 6**: Localization — Add all new translation keys.
7. **Step 7**: Testing — Visual testing at multiple resolutions, verify tooltip content accuracy.

### Alternatives Considered

- **Single 6-slot ItemGrid** — Rejected because `SlotMouseEntered`/`SlotMouseExited` event handling in custom pages is unverified, and distinguishing which slot was hovered requires the `@SlotIndex` value reference which adds codec complexity.
- **`ItemIcon` element per slot** — Rejected due to zero confirmed usage outside the type documentation listing. `ItemGrid` with single slots is the proven pattern.
- **Dynamic `appendInline()` for tooltip content** — Rejected for the equipment tooltip in favor of pre-built `Message` objects set on a Label, which is simpler and matches the existing modifier tooltip pattern. The `appendInline()` approach (used in HyforgedHud) is better suited for dynamic-length content in HUDs, not fixed-content tooltip overlays.

## Questions for User

None — all decisions have been made based on the spec and user decisions provided.
