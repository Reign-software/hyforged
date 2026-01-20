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
- Forging (corrupting)
  - Forging an item makes it unmodifiable (no further enchanting/disenchanting).
  - Forging adds a powerful random enchantment.
  - Forging has a flat, large Tradebar fee plus exotic materials.
  - UI clearly communicates irreversible nature and risks.
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
- Forging flow
  - Irreversible lock
  - Powerful random effect
  - Flat fee + exotic materials
- Eligibility and anti-exploit

## Change Log
- 2026-01-19: Initial version drafted.
