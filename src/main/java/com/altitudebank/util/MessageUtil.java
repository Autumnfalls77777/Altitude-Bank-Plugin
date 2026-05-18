package com.altitudebank.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * MessageUtil — helpers for formatting messages, amounts, and sending messages to players.
 */
public final class MessageUtil {

    private MessageUtil() {} // utility class

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    /**
     * Translates &amp; color codes into a Component and sends to the player.
     */
    public static void send(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        player.sendMessage(LEGACY.deserialize(message));
    }

    /**
     * Translates &amp; color codes to a Component.
     */
    public static Component colorize(String message) {
        if (message == null) return Component.empty();
        return LEGACY.deserialize(message);
    }

    /**
     * Formats a double to a human-readable currency string with commas.
     * Example: 1234567.89 → "1,234,567.89"
     *
     * @param amount       the value to format
     * @param decimalPlaces number of decimal places
     */
    public static String formatAmount(double amount, int decimalPlaces) {
        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimalPlaces > 0) {
            pattern.append(".");
            pattern.append("0".repeat(decimalPlaces));
        }
        DecimalFormat df = new DecimalFormat(pattern.toString(),
                DecimalFormatSymbols.getInstance(Locale.US));
        return df.format(amount);
    }

    /**
     * Parses a player-supplied amount string that may contain commas or spaces.
     * @throws NumberFormatException if the string is not a valid positive number
     */
    public static double parseAmount(String input) throws NumberFormatException {
        // Strip commas and spaces
        String cleaned = input.replace(",", "").replace(" ", "").trim();
        double value = Double.parseDouble(cleaned);
        if (value <= 0) throw new NumberFormatException("Amount must be positive.");
        if (Double.isInfinite(value) || Double.isNaN(value))
            throw new NumberFormatException("Invalid number.");
        return value;
    }

    /**
     * Replaces simple {key} style placeholders in a template string.
     * Usage: replace("Hello {name}!", "name", "World")
     */
    public static String replace(String template, String... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("Pairs must be even.");
        for (int i = 0; i < pairs.length; i += 2) {
            template = template.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return template;
    }
}
