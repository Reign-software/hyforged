# Requirements: Trading & Marketplace

## Vision
- Provide secure, server-controlled player trading for items, runes, and Tradebars, including direct trades and an in-world marketplace/auction mechanism suitable for player bases.

## Goals
- Tradeable assets
  - Players can trade:
    - Items (including affixes/rarity/enchantments)
    - Runes
    - Tradebars
  - Trades preserve full item metadata and always reflect server-authoritative item state.
- Direct player-to-player trading
  - Provide a secure trade window/interface.
  - Both parties can review items and Tradebars offered.
  - Trade completes only after explicit confirmation from both parties.
  - Support a “lock” step and a final “confirm” step to prevent last-moment swaps.
- Marketplace / auction house
  - Provide a marketplace table block that can be placed in a player base.
  - Players can list items for sale in exchange for Tradebars.
  - Listings include:
    - Item preview (rarity, affixes, enchantments)
    - Price in Tradebars
    - Duration / expiration (configurable)
  - Purchases are server authoritative and use atomic transactions.
- Server-side control and safeguards
  - Trading is controlled server-side.
  - Safeguards include:
    - Atomic exchange (either both sides receive, or neither)
    - Validation at confirm-time (items still owned, sufficient currency)
    - Anti-dupe protections and rollback on error
    - Optional rate limits / trade cooldowns
  - Trades and marketplace purchases are auditable with rate-limited logs.
- UX requirements
  - Clear display of what is being exchanged.
  - Clear warnings for irreversible actions (e.g., buying a forged item).
  - Optional “value hints” are explicitly marked as estimates (if provided).

## Non-Goals
- Client-authoritative trading.
- Trust-based trades without confirmation.
- Cross-server trading.

## Quality Attributes
- Security: prevents fraud, last-second swap scams, and duplication.
- Reliability: atomic transactions and clear error handling.
- Performance: marketplace queries and listings are efficient at scale.
- Observability: audit logs for disputes.

## Feature Index
- Direct trade
  - Trade UI and confirmation flow
  - Validation rules
- Marketplace
  - Listing creation
  - Search/browse
  - Purchase flow
  - Expiration handling
- Security
  - Atomicity
  - Anti-dupe
  - Rate limiting

## Change Log
- 2026-01-19: Initial version drafted.
