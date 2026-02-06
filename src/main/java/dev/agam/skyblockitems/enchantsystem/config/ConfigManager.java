package dev.agam.skyblockitems.enchantsystem.config;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
     * Add a material to the blacklist.
     */
    public void addToBlacklist(String materialName) {
        List<String> blacklist = getBlacklist();
        if (!blacklist.contains(materialName.toUpperCase())) {
            blacklist.add(materialName.toUpperCase());
            plugin.getConfig().set("blacklist", blacklist);
            plugin.saveConfig();
        }
    }

    /**
     * Remove a material from the blacklist.
     */
    public void removeFromBlacklist(String materialName) {
        List<String> blacklist = getBlacklist();
        if (blacklist.remove(materialName.toUpperCase())) {
            plugin.getConfig().set("blacklist", blacklist);
            plugin.saveConfig();
        }
    }

    /**
     * Check if a material is blacklisted.
     */
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
