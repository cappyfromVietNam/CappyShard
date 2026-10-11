package com.cappy.shards.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class ChatUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private ChatUtils() {}

    /**
     * Safely parses a string with MiniMessage tags or legacy color codes into an Adventure Component.
     * Guaranteed never to throw an exception!
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // If the string contains section signs (§), convert legacy section to Component
        if (text.contains("§")) {
            return LEGACY_SECTION.deserialize(text);
        }

        // If string contains legacy & but no MiniMessage tags
        if (text.contains("&") && !text.contains("<") && !text.contains(">")) {
            return LEGACY_AMPERSAND.deserialize(text);
        }

        // If string contains both & and MiniMessage tags, convert & codes to MiniMessage tags
        String processed = text;
        if (processed.contains("&")) {
            processed = processed.replace("&0", "<black>")
                    .replace("&1", "<dark_blue>")
                    .replace("&2", "<dark_green>")
                    .replace("&3", "<dark_aqua>")
                    .replace("&4", "<dark_red>")
                    .replace("&5", "<dark_purple>")
                    .replace("&6", "<gold>")
                    .replace("&7", "<gray>")
                    .replace("&8", "<dark_gray>")
                    .replace("&9", "<blue>")
                    .replace("&a", "<green>")
                    .replace("&b", "<aqua>")
                    .replace("&c", "<red>")
                    .replace("&d", "<light_purple>")
                    .replace("&e", "<yellow>")
                    .replace("&f", "<white>")
                    .replace("&l", "<bold>")
                    .replace("&m", "<strikethrough>")
                    .replace("&n", "<underlined>")
                    .replace("&o", "<italic>")
                    .replace("&r", "<reset>");
        }

        try {
            return MINI_MESSAGE.deserialize(processed);
        } catch (Exception e) {
            // Fallback safely to legacy ampersand, then plain text
            try {
                return LEGACY_AMPERSAND.deserialize(text);
            } catch (Exception ignored) {
                return Component.text(text);
            }
        }
    }

    /**
     * Safely parses with custom TagResolvers.
     */
    public static Component parse(String text, TagResolver... resolvers) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        try {
            return MINI_MESSAGE.deserialize(text, resolvers);
        } catch (Exception e) {
            return parse(text);
        }
    }

    /**
     * Sends a parsed message to a CommandSender.
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null && !message.isEmpty()) {
            sender.sendMessage(parse(message));
        }
    }
}
