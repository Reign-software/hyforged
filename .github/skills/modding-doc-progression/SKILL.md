---
name: modding-doc-progression
description: Doc-derived guidance for the Hyforged Progression system. Use when adding XP curves, classes, level rewards, or XP awards from Modding_Doc/Progression. Triggers - progression, xp, level, class, curve, weapon tags, modding doc.
---

# Modding Doc: Progression

This skill summarizes Modding_Doc/Progression at a high level and links to the full references.

## Documentation References

- [Progression System Overview](../../../Modding_Doc/Progression/README.md) — XP curves, classes, rewards, configuration

## Doc-Derived How-To (Adding Progression Content)

1. Define XP curves in `src/main/resources/Server/<YourMod>/Progression/` using namespaced `id`, `type`, `baseXp`, `exponent`, and `maxLevel`.
2. Define class data in `src/main/resources/Server/<YourMod>/Stats/Classes/` with `weaponTagFamilies` and `levelRewards`.
3. Configure XP sources and caps in `src/main/resources/Server/<YourMod>/Progression/XPConfig.json`.
4. Add XP awards to objectives using the `hyforged:xp_award` completion type or explicit XP amounts.
5. Integrate via progression events or `ProgressionStatBridge` instead of hard-coded level checks.

Notes:
- Keep everything data-driven and namespaced; avoid hard-coded values.
- Ensure weapon tags align with existing tag families in your item data.