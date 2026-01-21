---
name: modding-doc-scaling
description: Doc-derived guidance for Hyforged monster scaling. Use when adding world scaling or monster scaling configs from Modding_Doc/Scaling. Triggers - scaling, monster scaling, world scaling, npc level, modding doc.
---

# Modding Doc: Scaling

This skill summarizes Modding_Doc/Scaling at a high level and links to the full references.

## Documentation References

- [Monster Scaling Overview](../../../Modding_Doc/Scaling/README.md) — World scaling, monster scaling, schema details

## Doc-Derived How-To (Adding Scaling)

1. Define world scaling configs in `src/main/resources/Server/<YourMod>/Combat/WorldScaling/` to control level-by-distance behavior.
2. Define monster scaling configs in `src/main/resources/Server/<YourMod>/Combat/MonsterScaling/` with `AppliesTo` and `ScaledStats` entries.
3. Use stat IDs from the Stats system (including custom stats) for `ScaledStats`.
4. Validate scaling with `MonsterScalingService` or in-game testing.

Notes:
- Keep everything data-driven and namespaced; avoid hard-coded values.
- Favor small, composable configs per monster family to keep tuning manageable.