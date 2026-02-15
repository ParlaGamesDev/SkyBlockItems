package dev.agam.skyblockitems.enchantsystem.managers;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant.EnchantStat;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Manages custom enchantments created through MMOEnchants.
 */
public class CustomEnchantManager {

    private final SkyBlockItems plugin;
    private final Map<String, CustomEnchant> customEnchants = new HashMap<>();
    private File configFile;
    private FileConfiguration config;

    public CustomEnchantManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        loadConfig();
        loadEnchants();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "custom-enchants.yml");
        if (!configFile.exists()) {
            plugin.saveResource("custom-enchants.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // FORCE MERGE - Copy missing keys from default to active config
        InputStream defConfigStream = plugin.getResource("custom-enchants.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration
                    .loadConfiguration(
                            new java.io.InputStreamReader(defConfigStream, java.nio.charset.StandardCharsets.UTF_8));

            boolean changed = false;
            for (String key : defConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defConfig.get(key));
                    changed = true;
                }
            }

            if (changed) {
                saveConfig();
            }
        }
    }

    public void loadEnchants() {
        customEnchants.clear();

        ConfigurationSection enchantsSection = config.getConfigurationSection("custom-enchants");
        if (enchantsSection == null)
            return;

        for (String id : enchantsSection.getKeys(false)) {
            ConfigurationSection section = enchantsSection.getConfigurationSection(id);
            if (section == null)
                continue;

            CustomEnchant enchant = new CustomEnchant(id);
            enchant.setDisplayName(section.getString("display-name", "&f" + id));
            enchant.setDescription(section.getString("description", "&7No description"));

            try {
                enchant.setMaterial(Material.valueOf(section.getString("material", "ENCHANTED_BOOK")));
            } catch (Exception e) {
                enchant.setMaterial(Material.ENCHANTED_BOOK);
            }

            enchant.setTargets(section.getStringList("targets"));
            enchant.setMaxLevel(section.getInt("max-level", 5));
            List<String> conflicts = plugin.getConfig().getStringList("conflicts-" + id.toLowerCase());
            if (conflicts == null || conflicts.isEmpty()) {
                conflicts = section.getStringList("conflicts");
            }
            enchant.setConflicts(conflicts);
            enchant.setRequiredEnchantingLevel(
                    plugin.getConfig().getInt("required-enchanting-level-" + id.toLowerCase(),
                            section.getInt("required-enchanting-level", 0)));
            enchant.setEnabled(section.getBoolean("enabled", true));

            // Load stats
            ConfigurationSection statsSection = section.getConfigurationSection("stats");
            if (statsSection != null) {
                for (String statName : statsSection.getKeys(false)) {
                    try {
                        EnchantStat stat = EnchantStat.valueOf(statName);
                        double perLevel = statsSection.getDouble(statName + ".per-level", 0);
                        enchant.setStat(stat, perLevel);
                    } catch (Exception ignored) {
                    }
                }
            }

            // Load xp costs
            ConfigurationSection levelsSection = section.getConfigurationSection("levels");
            if (levelsSection != null) {
                for (String levelStr : levelsSection.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelStr);
                        int xpCost = levelsSection.getInt(levelStr + ".xp-cost", level * 10);
                        enchant.setXpCost(level, xpCost);
                    } catch (Exception ignored) {
                    }
                }
            }

            customEnchants.put(id.toLowerCase(), enchant);
        }

        plugin.getLogger().info("Loaded " + customEnchants.size() + " custom enchants.");
    }

    public void saveEnchant(CustomEnchant enchant) {
        String path = "custom-enchants." + enchant.getId();

        config.set(path + ".display-name", enchant.getDisplayName());
        config.set(path + ".description", enchant.getDescription());
        config.set(path + ".material", enchant.getMaterial().name());
        config.set(path + ".targets", enchant.getTargets());
        config.set(path + ".max-level", enchant.getMaxLevel());
        config.set(path + ".conflicts", enchant.getConflicts());
        config.set(path + ".required-enchanting-level", enchant.getRequiredEnchantingLevel());

        // Save stats
        for (Map.Entry<EnchantStat, Double> entry : enchant.getStats().entrySet()) {
            config.set(path + ".stats." + entry.getKey().name() + ".per-level", entry.getValue());
        }

        // Save xp costs
        for (Map.Entry<Integer, Integer> entry : enchant.getXpCosts().entrySet()) {
            config.set(path + ".levels." + entry.getKey() + ".xp-cost", entry.getValue());
        }

        saveConfig();
        customEnchants.put(enchant.getId().toLowerCase(), enchant);
    }

    public void deleteEnchant(String id) {
        config.set("custom-enchants." + id, null);
        saveConfig();
        customEnchants.remove(id.toLowerCase());
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save custom-enchants.yml!");
        }
    }

    public CustomEnchant getEnchant(String id) {
        return customEnchants.get(id.toLowerCase());
    }

    public CustomEnchant createEnchant(String id) {
        CustomEnchant enchant = new CustomEnchant(id);
        customEnchants.put(id.toLowerCase(), enchant);
        saveEnchant(enchant);
        return enchant;
    }

    public Collection<CustomEnchant> getAllEnchants() {
        return customEnchants.values();
    }

    public boolean exists(String id) {
        return customEnchants.containsKey(id.toLowerCase());
    }

    public void reload() {
        loadConfig();
        loadEnchants();
    }
}
