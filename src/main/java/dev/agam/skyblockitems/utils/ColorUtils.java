package dev.agam.skyblockitems.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates both legacy (&c) and hex (&#RRGGBB) color codes.
     */
    public static String translate(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Translate Hex colors first
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + color).toString());
        }
        matcher.appendTail(buffer);
        String hexTranslated = buffer.toString();

        // Translate Legacy colors (&c)
        return ChatColor.translateAlternateColorCodes('&', hexTranslated);
    }
}
