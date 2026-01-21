---
name: modding-doc-combat
description: Doc-derived guidance for the Hyforged Combat system. Use when adding damage types, ailments, healing, or integrating CombatService from Modding_Doc/Combat. Triggers - combat, damage, ailment, critical, block, healing, DamageSpec, CombatService, modding doc.
---

# Modding Doc: Combat

This skill summarizes Modding_Doc/Combat at a high level and links to the full references.

## Documentation References

- [Combat System Overview](../../../Modding_Doc/Combat/README.md) — Concepts, pipeline, JSON schemas, configuration
- [Combat API Reference](../../../Modding_Doc/Combat/API.md) — CombatService, DamageSpec, HealingService

## Doc-Derived How-To (Adding Combat Features)

1. Define damage type extensions in `src/main/resources/Server/<YourMod>/Damage/` with resistance and penetration stat IDs plus element tags.
2. Define ailments in `src/main/resources/Server/<YourMod>/Combat/Ailments/` for threshold-based status effects.
3. For abilities or skills, build a `DamageSpec` and call `CombatService.applyDamage(...)` using a `CommandBuffer`.
4. For healing, use `HealingSpec` with `HealingService.applyHealing(...)` (bypasses combat defenses).
5. Validate using `CombatConfig` debug logging and the combat log commands described in the Modding_Doc.

Notes:
- Keep everything data-driven and namespaced; avoid hard-coded values.
- Ensure related stats exist in the Stats system (resistance, penetration, crit, block, healing).