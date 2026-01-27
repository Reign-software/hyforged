# Requirements: Currency System (Tradebars)

## Vision
- Provide Tradebars as the core ARPG currency that integrates with loot, selling, crafting/enchanting, passive refunds, and player trading—while being secure, intuitive, and fully server-controlled.

## Goals
- Tradebars definition
  - Tradebars are the primary currency.
  - Tradebars exist as an item that can be stored in inventory.
  - Tradebars stack up to 10,000 per inventory slot.
- Earning Tradebars
  - Tradebars can be found in chests (never dropped from combat).
  - Selling items grants Tradebars; every sellable item returns some Tradebars.
  - Sell values are data-driven: calculated by rarity + affixes with optional per-item override.
- Spending Tradebars
  - Tradebars can be spent on:
    - Refunding passive points
    - Crafting/enchanting/disenchanting/forging
    - Player trading fees (optional/configurable)
- Market Stall block
  - Provide a Market Stall block where players can sell items at calculated market price.
  - UI shows calculated value before confirming sale.
  - (Future) Other players can view listed items for purchase.
- Storage vault block
  - Provide a vault block that stores only Tradebars.
  - Vault is upgradable via data-driven tier configuration.
  - Vault is single-owner (player who placed it).
  - Only owner can access or destroy the vault.
  - Other players cannot destroy the vault block.
- Security and authority
  - Currency changes are server authoritative.
  - All currency transactions are fully auditable (timestamp, transaction ID, before/after balance, reason).
  - Prevent duplication exploits via atomic transaction rules and rollback on failure.

## Non-Goals
- Multiple parallel currencies at launch (Tradebars only).
- Client-side authoritative currency representation.
- Tradebars dropping from combat (chests and selling only).
- Pouch system (vault-only extended storage).

## Quality Attributes
- Integrity: no dupes; atomic transactions.
- Clarity: pouch/vault UX prevents confusion about where currency is stored.
- Extensible: costs and sources are configurable.
- Performance: currency operations are O(1) per transaction.

## Feature Index
- Item model
  - Stack size (10,000)
  - Inventory representation
- Sources
  - Chest loot
  - Item selling (Market Stall)
- Sinks
  - Passive refunds
  - Enchanting flows
  - Trading
- Storage
  - Vault block (upgradable, owner-only)
- Market Stall
  - Sell items at calculated market price
  - Rarity + affix-based value calculation
  - Optional per-item override
- Transaction safety
  - Atomicity and full audit logging

## Change Log
- 2026-01-27: Updated with clarified decisions (10K stack, vault-only storage, Market Stall, chest-only sources, full audit). Spec created.
- 2026-01-19: Initial version drafted.
