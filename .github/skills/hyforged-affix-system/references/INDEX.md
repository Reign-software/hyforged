# Affix System References

This directory contains references to the canonical documentation for the Hyforged Affix System.

## Primary Documentation

| Document | Location | Purpose |
|----------|----------|---------|
| System Overview | [Modding_Doc/Affixes/README.md](../../../../Modding_Doc/Affixes/README.md) | Concepts, JSON schemas, best practices |
| API Reference | [Modding_Doc/Affixes/API.md](../../../../Modding_Doc/Affixes/API.md) | Complete programmatic API documentation |

## JSON Schema Locations

| Schema Type | Path Pattern |
|-------------|--------------|
| Affix Definitions | `src/main/resources/Server/Hyforged/Affixes/*.json` |
| Affix Pools | `src/main/resources/Server/Hyforged/AffixPools/*.json` |
| Quality Rules | `src/main/resources/Server/Hyforged/QualityAffixRules/*.json` |
| Affix Types | `src/main/resources/Server/Hyforged/AffixTypes/*.json` |

## Source Code Locations

| Component | Package |
|-----------|---------|
| Public API | `reign.software.hyforged.affix.api` |
| Models | `reign.software.hyforged.affix.model` |
| Registry | `reign.software.hyforged.affix.registry` |
| Service | `reign.software.hyforged.affix.service` |

## Related Systems

- **Stats System** — Affixes modify stats via `StatId` and `HyforgedModifier`
  - [Stats README](../../../../Modding_Doc/Stats/README.md)
  - [Stats API](../../../../Modding_Doc/Stats/API.md)
