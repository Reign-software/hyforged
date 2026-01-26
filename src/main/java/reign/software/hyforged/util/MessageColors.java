package reign.software.hyforged.util;

import com.hypixel.hytale.server.core.Message;

import javax.annotation.Nonnull;

/**
 * Centralized color constants and message builders for Hyforged commands.
 * <p>
 * Hytale uses 6-character hex colors (e.g., "#FF5555") via Message.color().
 * This replaces Minecraft-style color codes (e.g., "") which Hytale does not support.
 */
public final class MessageColors {

    // Standard colors (based on Minecraft color code equivalents)
    public static final String ERROR = "#FF5555";       //  - Red
    public static final String SUCCESS = "#55FF55";     //  - Green
    public static final String WARNING = "#FFFF55";     //  - Yellow
    public static final String GOLD = "#FFAA00";        //  - Gold/Orange
    public static final String GRAY = "#AAAAAA";        //  - Gray
    public static final String WHITE = "#FFFFFF";       //  - White
    public static final String AQUA = "#55FFFF";        //  - Aqua/Cyan
    public static final String PURPLE = "#FF55FF";      //  - Light Purple/Magenta

    private MessageColors() {
        // Utility class
    }

    /**
     * Creates an error message (red).
     */
    @Nonnull
    public static Message error(@Nonnull String text) {
        return Message.raw(text).color(ERROR);
    }

    /**
     * Creates a success message (green).
     */
    @Nonnull
    public static Message success(@Nonnull String text) {
        return Message.raw(text).color(SUCCESS);
    }

    /**
     * Creates a warning message (yellow).
     */
    @Nonnull
    public static Message warning(@Nonnull String text) {
        return Message.raw(text).color(WARNING);
    }

    /**
     * Creates a header message (gold).
     */
    @Nonnull
    public static Message header(@Nonnull String text) {
        return Message.raw(text).color(GOLD);
    }

    /**
     * Creates a label message (gray).
     */
    @Nonnull
    public static Message label(@Nonnull String text) {
        return Message.raw(text).color(GRAY);
    }

    /**
     * Creates a value message (white).
     */
    @Nonnull
    public static Message value(@Nonnull String text) {
        return Message.raw(text).color(WHITE);
    }

    /**
     * Creates an info message (aqua).
     */
    @Nonnull
    public static Message info(@Nonnull String text) {
        return Message.raw(text).color(AQUA);
    }

    /**
     * Strips all Minecraft-style color codes from a string.
     * Use this for messages that don't need coloring.
     */
    @Nonnull
    public static String stripColorCodes(@Nonnull String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}
