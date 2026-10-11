package com.cappy.shards.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ChatUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    // High performance LRU cache for parsed components (up to 1,000 entries)
    private static final int MAX_CACHE_SIZE = 1000;
    private static final Map<String, Component> PARSE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, Component>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private ChatUtils() {}

    /**
     * Safely parses a string with MiniMessage tags or legacy color codes into an Adventure Component.
     * Caches results in memory to avoid repetitive CPU deserialization.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component cached = PARSE_CACHE.get(text);
        if (cached != null) {
            return cached;
        }

        Component result = computeParse(text);
        PARSE_CACHE.put(text, result);
        return result;
    }

    private static Component computeParse(String text) {
        if (text.contains("§")) {
            return LEGACY_SECTION.deserialize(text);
        }

        if (text.contains("&") && !text.contains("<") && !text.contains(">")) {
            return LEGACY_AMPERSAND.deserialize(text);
        }

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

    public static void clearCache() {
        PARSE_CACHE.clear();
    }
}
