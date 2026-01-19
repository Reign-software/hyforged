# Inventory Management (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/inventory-management

## Purpose
Access and modify player inventories and UI pages.

## Key concepts
- Player inventory is accessed via `Player.getInventory()`.
- Inventory “pages” are controlled through a `PageManager` and `Page` enum.
- `ItemStack` represents items, quantity, and optional metadata/durability.
- `ItemContainer` allows adding/removing items and slot‑level operations.

## Related server areas
- Inventory types and pages live under server core inventory/ui packages.
