package dev.agam.skyblockitems.enchantsystem.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility for handling HEX colors and legacy color codes.
 */
public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("(&#([A-Fa-f0-9]{6}))|(<#([A-Fa-f0-9]{6}))>");
    private static final Pattern GRADIENT_PATTERN = Pattern
            .compile("<gradient:(#([A-Fa-f0-9]{6})):(#([A-Fa-f0-9]{6}))>(.*?)</gradient>");

    /**
     * Translates HEX colors (<#HEX>), gradients, and legacy color codes (&) into
     * Minecraft colors.
     */
    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // Handle Gradients
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(message);
        while (gradientMatcher.find()) {
            String color1 = gradientMatcher.group(1);
            String color2 = gradientMatcher.group(3);
            String text = gradientMatcher.group(5);
            message = message.replace(gradientMatcher.group(0), applyGradient(text, color1, color2));
        }

        // Handle HEX colors: &#RRGGBB or <#RRGGBB>
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            builder.append(ChatColor.translateAlternateColorCodes('&', message.substring(lastEnd, matcher.start())));
            String hexGroup = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);
            builder.append(ChatColor.of("#" + hexGroup));
            lastEnd = matcher.end();
        }
        builder.append(ChatColor.translateAlternateColorCodes('&', message.substring(lastEnd)));

        return builder.toString();
    }

    private static String applyGradient(String text, String color1, String color2) {
        java.awt.Color start = java.awt.Color.decode(color1);
        java.awt.Color end = java.awt.Color.decode(color2);
        StringBuilder sb = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            int r = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int g = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int b = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            sb.append(ChatColor.of(new java.awt.Color(r, g, b))).append(text.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Creates an ItemStack from a configuration section.
     */
    public static ItemStack getItemFromConfig(ConfigurationSection section, Material fallback) {
        if (section == null) {
            return new ItemStack(fallback);
        }

        String materialName = section.getString("material");
        Material mat = materialName != null ? Material.getMaterial(materialName.toUpperCase()) : fallback;
        if (mat == null)
            mat = fallback;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = section.getString("name");
            if (name != null) {
                meta.setDisplayName(colorize(name));
            }

            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(ColorUtils::colorize).collect(Collectors.toList()));
            }

            int customModelData = section.getInt("custom-model-data", -1);
            if (customModelData != -1) {
                meta.setCustomModelData(customModelData);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a decorative filler item (usually a glass pane) with no name.
     */
    public static ItemStack createFillerItem(Material material) {
        ItemStack item = new ItemStack(material != null ? material : Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
}
