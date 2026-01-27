---
name: modding-doc-overview
description: Doc-derived overview for Hyforged modding. Use for getting started guidance, namespaced IDs, folder structure, ECS patterns, and links to system guides. Triggers - modding doc, overview, getting started, namespaced ids.
---

# Modding Doc: Overview

This skill summarizes Modding_Doc/README and points to the system guides.

## Documentation References

- [Modding Overview](../../../Modding_Doc/README.md) — Getting started, folder structure, ECS guidance
- [Affix System](../../../Modding_Doc/Affixes/README.md) — Affix system guide
- [Combat System](../../../Modding_Doc/Combat/README.md) — Combat system guide
- [Currency System](../../../Modding_Doc/Currency/README.md) — Tradebar currency, vaults, market stalls
- [Passive Trees](../../../Modding_Doc/PassiveTrees/README.md) — Passive skill trees guide
- [Progression System](../../../Modding_Doc/Progression/README.md) — Progression guide
- [Scaling](../../../Modding_Doc/Scaling/README.md) — Monster scaling guide
- [Stats System](../../../Modding_Doc/Stats/README.md) — Stats system guide

## Doc-Derived How-To (Getting Started)

1. Use namespaced IDs (`namespace:name`) for all assets to avoid conflicts.
2. Place server-side assets under `src/main/resources/Server/<YourMod>/`.
3. Follow ECS patterns: components are data-only, systems contain logic, and use data-driven configs.
4. Select the relevant system guide (Affixes, Stats, Combat, Progression, Scaling) and follow its JSON schemas.

Notes:
- Keep everything data-driven and namespaced; avoid hard-coded values.
- Use the system-specific guides for detailed schemas and API usage.