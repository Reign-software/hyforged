# Requirements: Currency System (Tradebars)

## Vision
- Provide Tradebars as the core ARPG currency that integrates with loot, selling, crafting/enchanting, passive refunds, and player trading—while being secure, intuitive, and fully server-controlled.

## Goals
- Tradebars definition
  - Tradebars are the primary currency.
  - Tradebars exist as an item that can be stored in inventory.
  - Tradebars stack up to 500 per inventory slot.
- Earning Tradebars
  - Tradebars can be earned through combat, quests, and other configured activities.
  - Selling items grants Tradebars; every sellable item returns some Tradebars.
  - Sell values are data-driven and can scale with item rarity/affixes.
- Spending Tradebars
  - Tradebars can be spent on:
    - Refunding passive points
    - Crafting/enchanting/disenchanting/forging
    - Player trading fees (optional/configurable)
- Tradebar pouch
  - Provide a Tradebar pouch accessible from inventory UI.
  - Pouch stores Tradebars up to a capacity.
  - Pouch can be upgraded via crafting upgrades to increase capacity.
  - UI clearly shows current amount, capacity, and upgrade path.
- Storage block
  - Provide a vault block that stores only Tradebars.
  - Vault is upgradable to increase capacity.
  - Vault supports secure ownership/permissions (configurable for multiplayer bases).
- Security and authority
  - Currency changes are server authoritative.
  - All currency transactions are auditable (earn/spend/sell/trade) with rate-limited logging.
  - Prevent duplication exploits via atomic transaction rules and rollback on failure.

## Non-Goals
- Multiple parallel currencies at launch (Tradebars only).
- Client-side authoritative currency representation.

## Quality Attributes
- Integrity: no dupes; atomic transactions.
- Clarity: pouch/vault UX prevents confusion about where currency is stored.
- Extensible: costs and sources are configurable.
- Performance: currency operations are O(1) per transaction.

## Feature Index
- Item model
  - Stack size (500)
  - Inventory representation
- Sources
  - Combat/quest/activity rewards
  - Item selling values
- Sinks
  - Passive refunds
  - Enchanting flows
  - Trading
- Storage
  - Pouch (upgradable)
  - Vault block (upgradable)
- Transaction safety
  - Atomicity and auditing

## Change Log
- 2026-01-19: Initial version drafted.
