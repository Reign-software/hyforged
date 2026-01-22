---
name: modding-doc-buff-display
description: Buff/debuff display and single-file effect authoring for Hyforged. Use when adding new buffs/debuffs, status effect icons, or Hyforged effect JSON in Server/Hyforged/Effects.
---

# Modding Doc: Buff/Debuff Display (Hyforged)

Use this skill when creating or updating buffs/debuffs so they display correctly in Hytale while remaining single‑file and data‑driven.

## Core Rule (Single JSON)
- Define buffs/debuffs as **one JSON** per effect in:
  - `src/main/resources/Server/Hyforged/Effects/*.json`
- Do **not** add separate mapping JSONs. If you need Hyforged stat modifiers, include them in the same file.

## How Display Works
Hytale shows status effects using fields on `EntityEffect`:
- `StatusEffectIcon` – path to the icon (e.g., `UI/StatusEffects/Burn.png`)
- `Debuff` – `true` for debuffs, `false` for buffs
- `Duration` – default duration in seconds
- `Name` or `Locale` – optional UI name/translation key

Hyforged effects can embed a full `EntityEffect` definition or reference an existing Hytale effect ID.

## Recommended JSON Structure
```json
{
  "Id": "hyforged:haste",
  "EntityEffect": {
    "Duration": 6,
    "Debuff": false,
    "StatusEffectIcon": "UI/StatusEffects/Haste.png"
  },
  "HyforgedModifiers": [
    { "StatId": "hyforged:movement-speed-bps", "StackType": "INCREASED", "Amount": 1500 }
  ]
}
```

### Referencing an existing Hytale effect
```json
{
  "Id": "hyforged:burn",
  "EntityEffect": "Burn",
  "HyforgedModifiers": []
}
```

## HyforgedModifiers Rules
- `StatId` can be namespaced (e.g., `hyforged:max-health-flat`) or short (e.g., `max-health-flat`).
- `StackType` uses Hyforged stacking: `FLAT`, `INCREASED`, `MORE`, `CAP`.
- `Amount` is an integer; percent values use basis points (10000 = 100%).

## Assets & Icons
- Put icons under `src/main/resources/Common/UI/StatusEffects/` (or a similar UI path).
- Use the same path in `StatusEffectIcon`.

## Validation Checklist
- One JSON per effect in `Server/Hyforged/Effects/`.
- `EntityEffect` is present (embedded object or string ID).
- `StatusEffectIcon` and `Debuff` set appropriately for display.
- Any stat changes are in `HyforgedModifiers` within the same file.
