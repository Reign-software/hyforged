# Requirements: Enchanting, Runes, and Forging

## Vision
- Provide ARPG-style item enhancement through enchanting and rune progression, tightly integrated with Tradebars and the affix/stat systems, plus a high-stakes forging (“corrupting”) mechanic. (note that in rpgs like poe rank 1 is the best rank. Any given enchant with higher rank numbers is weaker. This allows for a clear progression path where lower rank numbers indicate more powerful enchantments. Also allows enchants to have different numbers of ranks while maintaing 1 is the best rank)

## Goals
- Enchanting
  - Players can enchant weapons, armor, and eligible equipment.
  - Enchantments leverage the item affix/modifier model (compatible with Stats System).
  - Enchanting requires:
    - Tradebars (cost scales by item rarity and enchantment power)
    - Appropriate enchanting materials
  - Enchanting UI clearly shows:
    - Costs (Tradebars + materials)
    - Possible outcomes / enchant pool (where appropriate)
    - Resulting stat changes
- Disenchanting and runes
  - Players can disenchant items to remove enchantments.
  - Disenchanting costs Tradebars (scales by rarity/power removed).
  - Disenchanting produces a rune that represents the extracted enchantment.
  - Runes are consumable to “learn” and progress that enchantment.
  - Rune progression rule:
    - If the rune is an upgrade, it lowers the player’s existing rune/enchantment tier by 1 (not granting a specific tier directly).
    - This encourages repeated disenchanting to progress desired enchants.
    - The consumed rune must be of the same enchantment type as the one being upgraded.
    - The tier of the consumed rune must be equal to or lower than the tier of the enchantment being upgraded.
    - If you do not have the enchantment from the consumed rune, the user gains that enchantment as if they have the highest tier of that enchantment. (if the rune is lower tier than the highest tier, they gain the highest tier + 1) Example: the rune is critical strike chance tier 15, the highest tier is 20, so upon consumption they gain critical strike chance tier 19.
  - Runes can be traded between players.
  - Applying enchants to an item can only be done in an empty affix slot.
- Item Upgrading
  - Players can use rare manuscripts found in the world to "upgrade" item quality.
  - Manuscripts are consumed on use.
  - Manuscripts have quality tiers that determine their effectiveness. (an epic manuscript can upgrade a rare item to epic quality. A rare manuscript can upgrade a common item to rare quality.) The new affix slots granted by the quailty incrase have a 50% chance to be populated by a random affix of the appropriate tier.
  - An even rarer manuscript called Empty manuscript can be used but it differs as it leaves the affixes unchanged and only upgrades the item's quality tier. Adding the additional affix slots for the new quality tier. allowing the player to use runes in the newly added affix slots.
  - Using a manuscript has a chance to modify the item's affixes.
  - Can only be used on non-forged items.
  - Tradebars are consumed when using a manuscript.
- Purging Runes
  - Purging runes can be found in the world.
  - Purging runes can be used to remove unwanted enchantments from items.
  - Purging runes are consumed on use.
  - There are different types of purging runes based on their quality.
    - Common: Removes a random single affix from an item.
    - Uncommon: Removes a random single affix from an item, with a higher chance to remove a higher-tier affix.
    - Rare: Removes all affixes from an item. 
    - Epic: Removes a specific affix chosen by the player.
- Forging (corrupting)
  - Forging an item makes it unmodifiable (no further enchanting/disenchanting).
  - Forging adds a powerful random enchantment.
  - Forging has a flat, large Tradebar fee plus exotic materials.
  - Forging can only be performed on non-forged items.
  - Forging is irreversible and permanently locks the item from further modification.
  - Forged items can still be repaired.
  - Forging has a small chance of modifying the item's existing affixes.
    - A table is needed to define the probabilities and potential outcomes of affix modifications during forging.
    - Upgrading affixes
    - Downgrading affixes
    - Re-rolling affixes (a chance the count of affixes changed)
  - UI clearly communicates irreversible nature and risks.
  - Item has a 10% chance to be destroyed during forging.
- Eligibility and safeguards
  - Define item eligibility rules for enchanting/disenchanting/forging.
  - Prevent exploiting by enchanting/disenchanting loops with net-positive currency/material returns.
  - All results are server authoritative and auditable.

## Non-Goals
- Allowing forged (“corrupted”) items to be later modified.
- Granting exact rune tiers directly from a single drop unless explicitly configured.

## Quality Attributes
- Balance-friendly: costs, pools, and power are configurable.
- Secure: server-only outcome selection.
- Explainable: UI communicates what changed and why.
- Extensible: new enchantments and rune types can be added.

## Feature Index
- Enchanting flow
  - Costs and materials
  - Enchant pools/outcomes
  - UI requirements
- Disenchanting flow
  - Costs
  - Rune generation
  - Rune progression model
  - Trading runes
- Item Upgrading flow
  - Manuscript consumption
  - Quality/Affix modification mechanics
- Forging flow
  - Irreversible lock
  - Powerful random effect
  - Flat fee + exotic materials
- Eligibility and anti-exploit

## Change Log
- 2026-01-19: Initial version drafted.
