# Formatting the Chat (Summary)

Source: https://hytalemodding.dev/en/docs/guides/plugin/chat-formatting

## Purpose
Customize chat message content and formatting for players.

## Key concepts
- Chat is handled via `PlayerChatEvent`.
- You can cancel the event, replace content, or customize formatting.
- Formatters output a `Message` object with colors and styles.

## Rich text option
- The guide highlights TinyMessage as an external rich‑text parser that can produce `Message` objects with tags for colors, gradients, and links.

## Related server areas
- Message classes under `com.hypixel.hytale.server.core.Message`.
- Chat event types under `com.hypixel.hytale.server.core.event` or `com.hypixel.hytale.event`.
