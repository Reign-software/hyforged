# Currency System (Tradebars)

The Hyforged currency system uses **Tradebars** as the universal currency for trading, passive refunds, enchanting, and crafting.

## Overview

Tradebars are a server-authoritative currency stored in player inventories. They can be:
- Obtained from world chest loot
- Earned by selling items at Market Stalls
- Spent on passive tree refunds
- Used for crafting and enchanting (future features)

## Item Configuration

The Tradebar item is defined in `Server/Hyforged/Items/Tradebar.json`:

```json
{
  "TranslationProperties": {
    "Name": "Tradebar",
    "Description": "The universal currency of the realm."
  },
  "Categories": ["Items.Currency", "Items.Hyforged"],
  "MaxStack": 10000,
  "Tags": {
    "Type": ["Currency", "Hyforged"]
  }
}
```

Key properties:
- **MaxStack**: 10000 - High stack size for convenient storage
- **Tags**: `Currency` and `Hyforged` for item filtering

## Adding Tradebars to Loot Tables

To add Tradebars to world loot (chests, encounters, etc.), reference the tiered drop tables or create inline entries.

### Using Tiered Drop Tables

Hyforged provides pre-configured currency drop tables at different tiers:

| Drop Table | Typical Use | Quantity Range |
|------------|-------------|----------------|
| `hyforged:Drops/Currency_Tier1` | Zone 1 encounters | 5-50 |
| `hyforged:Drops/Currency_Tier2` | Zone 2 encounters | 20-120 |
| `hyforged:Drops/Currency_Tier3` | Zone 3+ encounters | 50-300 |

Example: Including Tier 1 currency in a chest loot table:

```json
{
  "Container": {
    "Type": "Multiple",
    "Containers": [
      {
        "Type": "Reference",
        "DropTableId": "hyforged:Drops/Currency_Tier1"
      },
      // ... other loot entries
    ]
  }
}
```

### Inline Tradebar Entry

For custom quantity ranges, add a direct entry:

```json
{
  "Type": "Single",
  "Weight": 25,
  "Item": {
    "ItemId": "hyforged:Tradebar",
    "QuantityMin": 10,
    "QuantityMax": 50
  }
}
```

### Recommended Quantity Ranges by Zone

| Zone | Min | Max | Notes |
|------|-----|-----|-------|
| Zone 1 | 5 | 50 | Early game, frequent small drops |
| Zone 2 | 20 | 120 | Mid game, moderate drops |
| Zone 3 | 50 | 200 | Late game, larger drops |
| Zone 4+ | 100 | 500 | End game, significant drops |
| Bosses | 200 | 1000 | Boss encounters, scaling with difficulty |

## Sell Values

Items sold at Market Stalls yield Tradebars based on:
1. Base value from item definition
2. Rarity multiplier (Common → Legendary)
3. Affix count bonus

See `Server/Hyforged/Config/SellValueConfig.json` for configuration.

## Tradebar Vault

The Tradebar Vault is a placeable block that provides secure storage for Tradebars.

### Vault Item

Located at `Server/Hyforged/Items/Tradebar_Vault.json`. Place the vault block in the world to create personal storage.

### Vault Features

- **Owner Protection**: Only the player who placed the vault can access it
- **Upgradable Tiers**: Upgrade capacity from Tier 1 (50,000) to Tier 4 (500,000)
- **UI Interface**: Deposit, withdraw, and upgrade via the vault interface

### Vault Tiers

| Tier | Capacity | Upgrade Cost |
|------|----------|--------------|
| 1 | 50,000 | - |
| 2 | 100,000 | 5,000 Tradebars |
| 3 | 250,000 | 25,000 Tradebars |
| 4 | 500,000 | 100,000 Tradebars |

Configure tiers in `Server/Hyforged/Config/VaultUpgrades.json`.

## Market Stall

The Market Stall is a placeable block where players can sell items for Tradebars.

### Market Stall Item

Located at `Server/Hyforged/Items/Market_Stall.json`. Place in the world to create a public selling station.

### Selling Items

1. Interact with the Market Stall to open the interface
2. The UI displays all sellable items in your inventory
3. Click "Sell All" to sell all items and receive Tradebars
4. Tradebars are deposited directly into your inventory

### Sellable Items

- All items with a positive sell value can be sold
- Tradebars cannot be sold (they are currency)
- Items with `Sellable: false` cannot be sold

## Currency HUD

When players have Tradebars in their inventory, a HUD element displays:
- **Inventory Balance**: Current Tradebars in inventory
- **Vault Balance**: Tradebars stored in vaults (shown after vault access)
- **Total**: Combined inventory + vault balance

## Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/hyforged currency balance <player>` | - | View player's Tradebar balance |
| `/hyforged currency grant <player> <amount>` | `hyforged.admin.currency` | Grant Tradebars to player |
| `/hyforged currency audit <player> [count]` | `hyforged.admin.currency` | View recent transactions |

## CurrencyService API

For programmatic access, use `CurrencyService.get()`:

```java
// Get player balance
int balance = CurrencyService.get().getBalance(playerRef);

// Deposit Tradebars (e.g., from loot)
TransactionResult result = CurrencyService.get().deposit(
    playerRef, 
    100, 
    "chest_loot"
);

// Deduct Tradebars (e.g., for purchase)
TransactionResult result = CurrencyService.get().deduct(
    playerRef, 
    50, 
    "passive_refund:warrior_tree"
);

// Check transaction success
if (result.success()) {
    // Transaction completed
} else {
    // Handle failure: result.failureReason()
}
```

## Audit Logging

All currency transactions are logged to `logs/hyforged/currency_audit_YYYY-MM-DD.log`:

```
2026-01-27T09:00:00-05:00|tx-abc123|player-uuid|ADMIN_GRANT|500|0|500|admin_grant:Console
```

Format: `timestamp|transactionId|playerUUID|type|amount|before|after|reason`
