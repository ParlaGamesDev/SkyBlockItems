package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
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
    private String statFormat;

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

        reforgesConfig = new YamlConfiguration();
        try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(reforgesFile.toPath(),
                java.nio.charset.StandardCharsets.UTF_8)) {
            reforgesConfig.load(reader);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load reforges.yml with UTF-8 encoding!", e);
        }

        loadReforges();
        this.statFormat = reforgesConfig.getString("settings.stat-format", "&#FAE8B8({value})");
        loadMMOConfigs();
    }

    public String getStatFormat() {
        return statFormat;
    }

    /**
     * Attempts to find and load MMOItems translation files.
     */
    private void loadMMOConfigs() {
        if (!plugin.isMMOItemsEnabled())
            return;
        try {
            java.io.File mmoFolder = new java.io.File(plugin.getDataFolder().getParentFile(), "MMOItems");
            if (!mmoFolder.exists())
                return;

            java.io.File languageFolder = new java.io.File(mmoFolder, "language");

            // Prioritized list of files to search
            java.util.List<java.io.File> possiblePaths = new java.util.ArrayList<>();

            // 1. Root language folder (Highest Priority per user)
            possiblePaths.add(new java.io.File(languageFolder, "stats.yml"));

            // 2. Common language subfolders (Fallbacks)
            String[] commonLangs = { "Hebrew", "hebrew", "HEBREW", "English", "english" };
            for (String lang : commonLangs) {
                possiblePaths.add(new java.io.File(languageFolder, lang + "/stats.yml"));
            }

            // 3. Root MMOItems folder (Extra Fallback)
            possiblePaths.add(new java.io.File(mmoFolder, "stats.yml"));

            // 4. Recursive scan (Search all subfolders for stats.yml)
            if (languageFolder.exists() && languageFolder.isDirectory()) {
                scanDirForStats(languageFolder, possiblePaths);
            }

            for (java.io.File statFile : possiblePaths) {
                if (statFile.exists() && !statFile.isDirectory()) {
                    this.mmoStatsConfig = YamlConfiguration.loadConfiguration(statFile);

                    break;
                }
            }

            // Also load language.yml for general messages
            java.io.File generalLangFile = new java.io.File(languageFolder, "language.yml");
            if (generalLangFile.exists()) {
                this.mmoLanguageConfig = YamlConfiguration.loadConfiguration(generalLangFile);
            } else {
                // Check in subfolders for language.yml as well
                for (String lang : commonLangs) {
                    java.io.File f = new java.io.File(languageFolder, lang + "/language.yml");
                    if (f.exists()) {
                        this.mmoLanguageConfig = YamlConfiguration.loadConfiguration(f);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load MMOItems translation configs: " + e.getMessage());
        }
    }

    private void scanDirForStats(java.io.File dir, java.util.List<java.io.File> list) {
        java.io.File[] files = dir.listFiles();
        if (files == null)
            return;
        for (java.io.File f : files) {
            if (f.isDirectory()) {
                java.io.File stats = new java.io.File(f, "stats.yml");
                if (stats.exists())
                    list.add(stats);
                scanDirForStats(f, list); // Recurse
            }
        }
    }

    /**
     * Formats a stat ID into its translated display name.
     * Favors the MMOItems API for 100% accuracy.
     */
    public String formatStatName(String raw) {
        if (raw == null || raw.isEmpty())
            return raw;

        String cleanId = raw.replace("mmoitems_", "");
        String hyphenId = cleanId.toUpperCase().replace("_", "-");
        String underscoreId = cleanId.toUpperCase().replace("-", "_");

        // 1. Check messages.yml (via a new reforge.stat-names section)
        String configKey = "reforge.stat-names." + cleanId.toLowerCase();
        String translated = plugin.getConfigManager().getMessageRaw(configKey);
        // getMessageRaw returns the path if not found, check if it's different
        if (translated != null && !translated.equals(configKey)) {
            return ColorUtils.colorize(translated);
        }

        // 1.5 Fallback to internal common mappings
        java.util.Map<String, String> commonMappings = new java.util.HashMap<>();
        commonMappings.put("STRENGTH", "Strength");
        commonMappings.put("CRIT_DAMAGE", "Crit Damage");
        commonMappings.put("CRIT_CHANCE", "Crit Chance");
        commonMappings.put("HEALTH", "Health");
        commonMappings.put("DEFENSE", "Defense");
        commonMappings.put("SPEED", "מהירות");
        commonMappings.put("WALK_SPEED", "מהירות הליכה");
        commonMappings.put("MOVEMENT_SPEED", "מהירות הליכה");
        commonMappings.put("ATTACK_SPEED", "מהירות התקפה");
        commonMappings.put("INTELLIGENCE", "Intelligence");

        if (commonMappings.containsKey(underscoreId)) {
            return commonMappings.get(underscoreId);
        }

        // 2. Try Direct MMOItems API
        if (plugin.isMMOItemsEnabled()) {
            try {
                net.Indyuce.mmoitems.stat.type.ItemStat stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats()
                        .get(hyphenId);
                if (stat == null) {
                    stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(underscoreId);
                }
                if (stat != null) {
                    return dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.colorize(stat.getName());
                }
            } catch (Exception ignored) {
            }
        }

        // 3. Fallback to file-based scanning (legacy)
        FileConfiguration[] configs = { mmoStatsConfig, mmoLanguageConfig };
        String[] searchKeys = {
                hyphenId, underscoreId,
                hyphenId + ".name", underscoreId + ".name",
                "stat." + hyphenId + ".name", "stat." + underscoreId + ".name"
        };

        for (FileConfiguration cfg : configs) {
            if (cfg == null)
                continue;
            for (String key : searchKeys) {
                String val = cfg.getString(key);
                if (val != null && !val.isEmpty()) {
                    return dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.colorize(val);
                }
            }
        }

        // 4. Final fallback: Pretty name
        return Arrays.stream(underscoreId.split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Extracts a clean stat name from an MMOItems lore format string.
     * E.g. "&7Attack Damage: &f<plus>{value}" -> "Attack Damage"
     */
    private String extractStatNameFromFormat(String format) {
        if (format == null)
            return null;

        // Remove color codes (§ and &)
        String stripped = format.replaceAll("[§&][0-9a-fA-Fk-orK-OR]", "");
        // Remove hex color codes if present
        stripped = stripped.replaceAll("&#[0-9a-fA-F]{6}", "");
        stripped = stripped.replaceAll("<#[0-9a-fA-F]{6}>", "");

        // If there's a colon, take everything before it (this is standard in MMOItems
        // stats.yml)
        if (stripped.contains(":")) {
            return stripped.split(":")[0].trim();
        }

        // Otherwise, remove common MMOItems placeholders
        String name = stripped
                .replace("<plus>", "")
                .replace("{value}", "")
                .replace("#", "")
                .replace("%", "")
                .trim();

        return name;
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
        if (id == null)
            return null;

        String cleanId = id.trim().toLowerCase();

        // Alias handling for legacy Hebrew IDs or migration
        if (cleanId.equals("אגדי")) {
            return reforges.get("legendary_sword");
        }

        return reforges.get(cleanId);
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

        // Sharp Logic: Pool based excluded pool management
        if (excludeReforgeId != null) {
            String cleanExclude = excludeReforgeId.trim().toLowerCase();
            applicable = applicable.stream()
                    .filter(r -> !r.getId().equalsIgnoreCase(cleanExclude))
                    .collect(Collectors.toList());
        }

        if (applicable.isEmpty()) {
            return null; // Return null to signal no AVAILABLE alternatives
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
            String rarityReq, double cost,
            Map<String, Double> stats, List<String> enchants, ReforgeGem gem) {
        reforgesConfig.set("reforges." + id + ".display-name", displayName);
        reforgesConfig.set("reforges." + id + ".item-types", itemTypes);
        reforgesConfig.set("reforges." + id + ".rarity-requirement", rarityReq);
        reforgesConfig.set("reforges." + id + ".cost", cost);
        reforgesConfig.set("reforges." + id + ".stats", stats.isEmpty() ? null : stats);
        reforgesConfig.set("reforges." + id + ".enchants", enchants.isEmpty() ? null : enchants);
        reforgesConfig.set("reforges." + id + ".abilities", null); // Wipe abilities if they existed

        if (gem != null) {
            String path = "reforges." + id + ".gem.";
            reforgesConfig.set(path + "name", gem.getName());
            reforgesConfig.set(path + "lore", gem.getLore());
            reforgesConfig.set(path + "material", gem.getMaterial());
            reforgesConfig.set(path + "custom-model-data", gem.getCustomModelData());
        } else {
            reforgesConfig.set("reforges." + id + ".gem", null);
        }

        saveConfig();
        reload();
    }

    /**
     * Gets a gem item stack for the given gem ID.
     * Note: Gems are defined within individual reforges in our system.
     * 
     * @param gemId The gem ID to search for
     * @return The Gem ItemStack, or null if not found
     */
    public org.bukkit.inventory.ItemStack getGemItem(String gemId) {
        for (Reforge reforge : reforges.values()) {
            ReforgeGem gem = reforge.getGem();
            if (gem != null && gem.getId().equalsIgnoreCase(gemId)) {
                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(gem.getMaterial().toUpperCase());
                if (mat == null)
                    mat = org.bukkit.Material.STONE;

                org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(gem.getName());
                    meta.setLore(gem.getLore());
                    if (gem.getCustomModelData() != 0) {
                        meta.setCustomModelData(gem.getCustomModelData());
                    }

                    // Add a hidden tag to identify it as a reforge gem
                    meta.getPersistentDataContainer().set(
                            new org.bukkit.NamespacedKey(plugin, "reforge_gem_id"),
                            org.bukkit.persistence.PersistentDataType.STRING, gem.getId());

                    item.setItemMeta(meta);
                }
                return item;
            }
        }
        return null;
    }

    /**
     * Retrieves the symbol (icon) for a stat from MMOItems translation configs.
     * 
     * @param statIdRaw The raw stat ID
     * @return The symbol, or "■" as fallback
     */
    public String getStatSymbol(String statIdRaw) {
        if (mmoStatsConfig == null) {
            // Hardcoded fallbacks for common SkyBlock stats if config is missing
            String clean = statIdRaw.replace("mmoitems_", "").toUpperCase();
            if (clean.contains("ATTACK_DAMAGE"))
                return "⚔";
            if (clean.contains("HEALTH"))
                return "❤";
            if (clean.contains("STRENGTH"))
                return "❁";
            if (clean.contains("DEFENSE"))
                return "❈";
            if (clean.contains("SPEED"))
                return "✦";
            if (clean.contains("CRIT_CHANCE"))
                return "☣";
            if (clean.contains("CRIT_DAMAGE"))
                return "☠";
            if (clean.contains("INTELLIGENCE"))
                return "✎";
            return "■";
        }

        String cleanId = statIdRaw.replace("mmoitems_", "").toUpperCase().replace("-", "_");
        String hyphenId = cleanId.replace("_", "-");

        // Try various keys in stats.yml
        String[] keys = { hyphenId + ".symbol", cleanId + ".symbol", hyphenId + ".icon", cleanId + ".icon" };
        for (String key : keys) {
            String val = mmoStatsConfig.getString(key);
            if (val != null && !val.isEmpty()) {
                return val;
            }
        }

        // Try extracting from name if it contains a weird character at the start
        String name = formatStatName(statIdRaw);
        if (name != null && name.length() > 2) {
            String stripped = ColorUtils.stripColor(name).trim();
            if (!stripped.isEmpty() && !Character.isLetterOrDigit(stripped.charAt(0))) {
                return String.valueOf(stripped.charAt(0));
            }
        }

        return "■";
    }
}
