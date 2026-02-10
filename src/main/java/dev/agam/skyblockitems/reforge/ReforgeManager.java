package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages the reforge system - loading reforges from configuration,
 * providing access to reforges, and handling reforge application logic.
 */
public class ReforgeManager {

    private final SkyBlockItems plugin;
    private final Map<String, Reforge> reforges;
    private File reforgesFile;
    private FileConfiguration reforgesConfig;
    private FileConfiguration mmoStatsConfig;
    private FileConfiguration mmoLanguageConfig;

    public ReforgeManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.reforges = new HashMap<>();
    }

    /**
     * Loads or reloads the reforges configuration from reforges.yml.
     */
    public void loadConfig() {
        // Create reforges.yml if it doesn't exist
        reforgesFile = new File(plugin.getDataFolder(), "reforges.yml");
        if (!reforgesFile.exists()) {
            plugin.saveResource("reforges.yml", false);
        }

        reforgesConfig = YamlConfiguration.loadConfiguration(reforgesFile);
        loadReforges();
        loadMMOConfigs();
    }

    private void loadMMOConfigs() {
        if (!plugin.isMMOItemsEnabled())
            return;
        try {
            java.io.File mmoFolder = org.bukkit.Bukkit.getPluginManager().getPlugin("MMOItems").getDataFolder();
            java.io.File languageFolder = new java.io.File(mmoFolder, "language");

            // The user confirmed: MMOItems > language > stats.yml
            java.io.File statsFile = new java.io.File(languageFolder, "stats.yml");
            if (statsFile.exists()) {
                this.mmoStatsConfig = YamlConfiguration.loadConfiguration(statsFile);
                plugin.getLogger().info("Loaded MMOItems translations from language/stats.yml");
            } else {
                // Fallback to root or default language folders
                statsFile = new java.io.File(mmoFolder, "stats.yml");
                if (statsFile.exists()) {
                    this.mmoStatsConfig = YamlConfiguration.loadConfiguration(statsFile);
                    plugin.getLogger().info("Loaded MMOItems translations from stats.yml (root)");
                }
            }

            // Check for additional translation files in the language folder if stats.yml
            // didn't have everything
            if (languageFolder.exists() && languageFolder.isDirectory()) {
                java.io.File generalLangFile = new java.io.File(languageFolder, "language.yml");
                if (generalLangFile.exists() && !generalLangFile.isDirectory()) {
                    this.mmoLanguageConfig = YamlConfiguration.loadConfiguration(generalLangFile);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load MMOItems translation configs: " + e.getMessage());
        }
    }

    public String formatStatName(String raw) {
        String searchId = raw.toUpperCase().replace("_", "-");
        String lowerSearchId = searchId.toLowerCase();
        String lowerRaw = raw.toLowerCase();

        // 1. Try direct reading from MMOItems stats.yml or language.yml
        FileConfiguration[] configs = { mmoStatsConfig, mmoLanguageConfig };
        String[] keys = {
                searchId + ".name",
                lowerSearchId + ".name",
                raw + ".name",
                lowerRaw + ".name",
                "stat." + raw + ".name",
                "stat." + lowerRaw + ".name",
                "stat." + searchId + ".name",
                "stat." + lowerSearchId + ".name",
                "stat-name." + searchId,
                "stat-name." + lowerSearchId,
                "stat-name." + raw,
                "stat-name." + lowerRaw,
                "stat-id." + searchId,
                "stat-id." + raw,
                "stat-name." + searchId.replace("-", "_"),
                "stat-name." + searchId.replace("_", "-")
        };

        for (FileConfiguration cfg : configs) {
            if (cfg == null)
                continue;
            for (String key : keys) {
                String val = cfg.getString(key);
                if (val != null && !val.isEmpty())
                    return dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.colorize(val);
            }
        }

        // 2. Fallback to API if available
        if (plugin.isMMOItemsEnabled()) {
            try {
                net.Indyuce.mmoitems.stat.type.ItemStat stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats()
                        .get(searchId);
                if (stat == null)
                    stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(raw.toUpperCase());
                if (stat != null)
                    return dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.colorize(stat.getName());
            } catch (Exception ignored) {
            }
        }

        // 3. Last fallback: Professional Title Case
        String name = raw.replace("_", " ").replace("-", " ");
        return Arrays.stream(name.split(" "))
                .filter(w -> !w.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Loads all reforges from the configuration file.
     */
    private void loadReforges() {
        reforges.clear();

        ConfigurationSection reforgesSection = reforgesConfig.getConfigurationSection("reforges");
        if (reforgesSection == null) {
            plugin.getLogger().warning("No 'reforges' section found in reforges.yml!");
            return;
        }

        int loadedCount = 0;
        for (String reforgeId : reforgesSection.getKeys(false)) {
            try {
                ConfigurationSection reforgeSection = reforgesSection.getConfigurationSection(reforgeId);
                if (reforgeSection == null) {
                    plugin.getLogger().warning("Invalid reforge configuration for: " + reforgeId);
                    continue;
                }

                Reforge reforge = new Reforge(reforgeId, reforgeSection);
                reforges.put(reforgeId.toLowerCase(), reforge);
                loadedCount++;

                plugin.getLogger().fine("Loaded reforge: " + reforge.getId() + " - " + reforge.getDisplayName());

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading reforge: " + reforgeId, e);
            }
        }

        plugin.getLogger().info("Loaded " + loadedCount + " reforges from reforges.yml");
    }

    /**
     * Gets a reforge by its ID.
     * 
     * @param id The reforge ID (case-insensitive)
     * @return The Reforge object, or null if not found
     */
    public Reforge getReforge(String id) {
        return reforges.get(id.toLowerCase());
    }

    /**
     * Gets all loaded reforges.
     * 
     * @return Collection of all reforges
     */
    public Collection<Reforge> getAllReforges() {
        return new ArrayList<>(reforges.values());
    }

    /**
     * Gets all reforges compatible with a specific item type.
     * 
     * @param itemType The item type (e.g., "SWORD", "PICKAXE")
     * @return List of compatible reforges
     */
    public List<Reforge> getReforgesForItemType(String itemType) {
        return reforges.values().stream()
                .filter(reforge -> reforge.isCompatibleWith(itemType))
                .collect(Collectors.toList());
    }

    /**
     * Gets all reforges compatible with an item type that also meet the rarity
     * requirement.
     * 
     * @param itemType      The item type
     * @param currentRarity The current rarity of the item
     * @return List of applicable reforges
     */
    public List<Reforge> getApplicableReforges(String itemType, String currentRarity) {
        return reforges.values().stream()
                .filter(reforge -> reforge.isCompatibleWith(itemType))
                .filter(reforge -> reforge.meetsRarityRequirement(currentRarity))
                .collect(Collectors.toList());
    }

    /**
     * Gets a random reforge for the given item type and rarity, excluding the
     * specified reforge.
     * 
     * @param itemType         The item type
     * @param currentRarity    The current rarity
     * @param excludeReforgeId The reforge ID to exclude (current reforge), can be
     *                         null
     * @return A random applicable reforge, or null if none available
     */
    public Reforge getRandomReforge(String itemType, String currentRarity, String excludeReforgeId) {
        List<Reforge> applicable = getApplicableReforges(itemType, currentRarity);

        // Relax rarity requirement or ignore it for randomness
        // If we don't find enough applicable ones with strict rarity,
        // fallback to all compatible ones for that item type
        if (applicable.size() <= 1) {
            applicable = getReforgesForItemType(itemType);
        }

        // Exclude the current reforge to ensure we get a different one
        if (excludeReforgeId != null && applicable.size() > 1) {
            applicable = applicable.stream()
                    .filter(r -> !r.getId().equalsIgnoreCase(excludeReforgeId))
                    .collect(Collectors.toList());
        }

        if (applicable.isEmpty()) {
            return null;
        }

        Random random = new Random();
        return applicable.get(random.nextInt(applicable.size()));
    }

    /**
     * Saves the reforges configuration to file.
     */
    public void saveConfig() {
        try {
            reforgesConfig.save(reforgesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save reforges.yml", e);
        }
    }

    /**
     * Reloads the reforges configuration.
     */
    public void reload() {
        loadConfig();
    }

    /**
     * Get the reforges configuration for direct editing.
     */
    public FileConfiguration getConfig() {
        return reforgesConfig;
    }

    /**
     * Update or create a reforge in the configuration and reload.
     */
    public void saveReforge(String id, String displayName, List<String> itemTypes,
            String rarityReq, String rarityUpgrade, double cost,
            Map<String, Double> stats, List<String> enchants, List<String> abilities) {
        reforgesConfig.set("reforges." + id + ".display-name", displayName);
        reforgesConfig.set("reforges." + id + ".item-types", itemTypes);
        reforgesConfig.set("reforges." + id + ".rarity-requirement", rarityReq);
        reforgesConfig.set("reforges." + id + ".rarity-upgrade", rarityUpgrade);
        reforgesConfig.set("reforges." + id + ".cost", cost);
        reforgesConfig.set("reforges." + id + ".stats", stats.isEmpty() ? null : stats);
        reforgesConfig.set("reforges." + id + ".enchants", enchants.isEmpty() ? null : enchants);
        reforgesConfig.set("reforges." + id + ".abilities", abilities.isEmpty() ? null : abilities);

        saveConfig();
        reload();
    }
}
