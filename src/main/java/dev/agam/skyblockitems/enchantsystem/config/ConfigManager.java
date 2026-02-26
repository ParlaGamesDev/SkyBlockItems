package dev.agam.skyblockitems.enchantsystem.config;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages all configuration files for the plugin.
 */
public class ConfigManager {

    private final SkyBlockItems plugin;
    private FileConfiguration messagesConfig;
    private FileConfiguration enchantsConfig;

    public ConfigManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        // Load main config
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // Load messages.yml
        messagesConfig = loadYaml("messages.yml");

        // Load enchants.yml
        enchantsConfig = loadYaml("enchants.yml");
    }

    private FileConfiguration loadYaml(String fileName) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists())
            dataFolder.mkdirs();

        File file = new File(dataFolder, fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 1. Load internal defaults
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);

            // 2. FORCE MERGE - Copy missing keys from default to active config
            // This is required for getConfigurationSection to work on the main config
            // object
            for (String key : defConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defConfig.get(key));
                }
            }
        }

        return config;
    }

    /**
     * Get the prefix for messages.
     */
    public String getPrefix() {
        return getMessage("general.prefix");
    }

    /**
     * Get the blacklist from config.yml.
     */
    public List<String> getBlacklist() {
        return plugin.getConfig().getStringList("blacklist");
    }

    /**
     * Add an item to the blacklist (either specific or material-wide).
     */
    public void addToBlacklist(ItemStack item) {
        if (item == null || item.getType().isAir())
            return;

        List<String> blacklist = getBlacklist();
        String idToAdd;

        if (isSpecialItem(item)) {
            idToAdd = getSpecificId(item);
        } else {
            idToAdd = item.getType().name();
        }

        if (!blacklist.contains(idToAdd)) {
            blacklist.add(idToAdd);
            plugin.getConfig().set("blacklist", blacklist);
            plugin.saveConfig();
        }
    }

    /**
     * Remove an item from the blacklist.
     */
    public void removeFromBlacklist(ItemStack item) {
        if (item == null || item.getType().isAir())
            return;

        List<String> blacklist = getBlacklist();
        String specificId = getSpecificId(item);
        String materialName = item.getType().name();

        boolean removed = false;
        // Try removing specific first, then material
        if (blacklist.remove(specificId)) {
            removed = true;
        } else if (blacklist.remove(materialName)) {
            removed = true;
        }

        if (removed) {
            plugin.getConfig().set("blacklist", blacklist);
            plugin.saveConfig();
        }
    }

    /**
     * Check if an item is blacklisted.
     */
    public boolean isBlacklisted(ItemStack item) {
        if (item == null || item.getType().isAir())
            return false;

        List<String> blacklist = getBlacklist();

        // 1. Check for specific item ID
        if (blacklist.contains(getSpecificId(item)))
            return true;

        // 2. Check for global material blacklist
        if (blacklist.contains(item.getType().name()))
            return true;

        return false;
    }

    /**
     * Helper to check if an item has unique properties.
     */
    public boolean isSpecialItem(ItemStack item) {
        if (item == null || item.getType().isAir())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;

        // Custom name
        if (meta.hasDisplayName())
            return true;
        // Custom model data
        if (meta.hasCustomModelData())
            return true;
        // Unique lore (excluding rarity lines)
        if (meta.hasLore()) {
            List<String> cleanLore = plugin.getRarityManager().stripRarityLore(meta.getLore());
            if (!cleanLore.isEmpty())
                return true;
        }

        // MMOItems check
        if (plugin.getServer().getPluginManager().isPluginEnabled("MythicLib")) {
            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
            if (nbt.hasTag("MMOITEMS_ITEM_ID"))
                return true;
        }

        return false;
    }

    /**
     * Generates a unique ID for a specific item instance.
     */
    public String getSpecificId(ItemStack item) {
        if (item == null || item.getType().isAir())
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append(item.getType().name());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName())
                sb.append("|Name:").append(meta.getDisplayName());
            if (meta.hasLore()) {
                List<String> cleanLore = plugin.getRarityManager().stripRarityLore(meta.getLore());
                if (!cleanLore.isEmpty())
                    sb.append("|Lore:").append(String.join("", cleanLore));
            }
            if (meta.hasCustomModelData())
                sb.append("|CMD:").append(meta.getCustomModelData());
        }

        // MMOItems uniqueness
        if (plugin.getServer().getPluginManager().isPluginEnabled("MythicLib")) {
            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
            if (nbt.hasTag("MMOITEMS_ITEM_ID")) {
                sb.append("|MMO:").append(nbt.getString("MMOITEMS_ITEM_TYPE")).append(":")
                        .append(nbt.getString("MMOITEMS_ITEM_ID"));
            }
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return "SPECIFIC:" + item.getType().name() + ":" + hexString.toString().substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            return "SPECIFIC:" + item.getType().name() + ":" + Math.abs(sb.toString().hashCode());
        }
    }

    /**
     * Legacy support/Material-only check.
     * 
     * @deprecated Use isBlacklisted(ItemStack)
     */
    @Deprecated
    public void addToBlacklist(String materialName) {
        List<String> blacklist = getBlacklist();
        if (!blacklist.contains(materialName.toUpperCase())) {
            blacklist.add(materialName.toUpperCase());
            plugin.getConfig().set("blacklist", blacklist);
            plugin.saveConfig();
        }
    }

    /**
     * Legacy support/Material-only check.
     * 
     * @deprecated Use isBlacklisted(ItemStack)
     */
    @Deprecated
    public void removeFromBlacklist(String materialName) {
        List<String> blacklist = getBlacklist();
        if (blacklist.remove(materialName.toUpperCase())) {
            plugin.getConfig().set("blacklist", blacklist);
            plugin.saveConfig();
        }
    }

    /**
     * Legacy support/Material-only check.
     * 
     * @deprecated Use isBlacklisted(ItemStack)
     */
    @Deprecated
    public boolean isBlacklisted(String materialName) {
        return getBlacklist().contains(materialName.toUpperCase());
    }

    /**
     * Get a message from messages.yml and apply color codes.
     */
    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "&cMissing message: " + path);
        return ColorUtils.colorize(message);
    }

    /**
     * Get a message from messages.yml with replacements.
     */
    public String getMessage(String path, String... replacements) {
        String message = messagesConfig.getString(path, "&cMissing message: " + path);
        for (int i = 0; i < replacements.length; i += 2) {
            String target = replacements[i];
            String value = replacements[i + 1];
            message = message.replace(target, value);
        }
        return ColorUtils.colorize(message);
    }

    /**
     * Get a raw message without color translation.
     */
    public String getMessageRaw(String path) {
        return messagesConfig.getString(path, "Missing: " + path);
    }

    /**
     * Get a list of messages from messages.yml.
     */
    public List<String> getMessageList(String path) {
        return messagesConfig.getStringList(path);
    }

    /**
     * Get the enchants configuration.
     */
    public FileConfiguration getEnchantsConfig() {
        return enchantsConfig;
    }

    /**
     * Get the messages configuration.
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    /**
     * Get the main plugin configuration.
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * Reload all configurations.
     */
    public void reload() {
        loadConfigs();
    }
}
