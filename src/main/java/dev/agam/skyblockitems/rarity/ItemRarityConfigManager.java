package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ItemRarityConfigManager {

    private final SkyBlockItems plugin;
    private File raritiesFile;
    private FileConfiguration raritiesConfig;

    // Config Cache
    private boolean debugMode;
    private List<String> loreFormat;

    // Rarity Data
    private final Map<String, RarityDefinition> rarities = new HashMap<>();
    private final List<CustomRule> customRules = new ArrayList<>();

    public ItemRarityConfigManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Load config.yml settings
        this.debugMode = config.getBoolean("debug-mode", false);
        this.loreFormat = config.getStringList("lore-format");
        if (this.loreFormat.isEmpty()) {
            this.loreFormat = Arrays.asList("{lore}", "", "{rarity-prefix}");
        }

        // Load rarities.yml
        this.raritiesFile = new File(plugin.getDataFolder(), "rarities.yml");
        if (!raritiesFile.exists()) {
            plugin.saveResource("rarities.yml", false);
        }
        this.raritiesConfig = YamlConfiguration.loadConfiguration(raritiesFile);

        loadRarities();
        loadCustomRules();

        if (debugMode) {
            plugin.getLogger().info("ItemRarity Config Loaded: " + rarities.size() + " rarities, " + customRules.size()
                    + " custom rules.");
        }
    }

    private void loadRarities() {
        rarities.clear();
        ConfigurationSection section = raritiesConfig.getConfigurationSection("rarities");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection raritySection = section.getConfigurationSection(key);
            if (raritySection == null)
                continue;

            String identifier = raritySection.getString("identifier");
            String displayName = raritySection.getString("display-name");
            int priority = raritySection.getInt("priority", 0);

            if (identifier != null) {
                rarities.put(identifier, new RarityDefinition(identifier, displayName, priority));
            }
        }
    }

    private void loadCustomRules() {
        customRules.clear();
        ConfigurationSection section = raritiesConfig.getConfigurationSection("custom");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection ruleSection = section.getConfigurationSection(key);
            if (ruleSection == null)
                continue;

            String materialName = ruleSection.getString("material");
            String rarityId = ruleSection.getString("rarity");
            boolean noRarity = ruleSection.getBoolean("no-rarity", false);
            List<String> lore = ruleSection.getStringList("lore");

            String mmoType = ruleSection.getString("mmo-type");
            String mmoId = ruleSection.getString("mmo-id");

            if (rarityId != null || noRarity) {
                Material material = null;
                // Only require material if NOT using MMOItems (or as fallback?)
                // Actually, let's allow material to be null if MMOItems are defined?
                // But CustomRule expects material currently. Let's keep valid material check
                // for now if present.
                if (materialName != null) {
                    try {
                        material = Material.valueOf(materialName.toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                customRules.add(new CustomRule(key, material, rarityId, lore, noRarity, mmoType, mmoId));
            }
        }
    }

    public void addCustomRule(Material material, String rarityId, List<String> lore, String mmoType, String mmoId) {
        String ruleName;
        if (mmoType != null && mmoId != null) {
            ruleName = "mmo_" + mmoType.toLowerCase() + "_" + mmoId.toLowerCase();
        } else {
            ruleName = material.name().toLowerCase() + "_" + rarityId.toLowerCase();
        }

        // Update in-memory: Remove logic implies checking collisions.
        // If mmoType is set, we check against that.
        customRules.removeIf(r -> {
            if (mmoType != null && mmoId != null) {
                return mmoType.equalsIgnoreCase(r.mmoItemType) && mmoId.equalsIgnoreCase(r.mmoItemId);
            }
            return r.material == material && r.mmoItemType == null; // Only replace GENERIC match if this is generic
        });

        customRules.add(new CustomRule(ruleName, material, rarityId, lore, false, mmoType, mmoId));

        // Update File
        if (raritiesConfig.getConfigurationSection("custom") == null) {
            raritiesConfig.createSection("custom");
        }

        String path = "custom." + ruleName;
        raritiesConfig.set(path + ".material", material.name());
        raritiesConfig.set(path + ".rarity", rarityId);
        raritiesConfig.set(path + ".lore", lore);
        raritiesConfig.set(path + ".no-rarity", null);
        raritiesConfig.set(path + ".mmo-type", mmoType);
        raritiesConfig.set(path + ".mmo-id", mmoId);

        saveRarities();
    }

    // Legacy support
    public void addCustomRule(Material material, String rarityId, List<String> lore) {
        addCustomRule(material, rarityId, lore, null, null);
    }

    public void addNoRarityRule(Material material, String mmoType, String mmoId) {
        String ruleName;
        if (mmoType != null && mmoId != null) {
            ruleName = "mmo_" + mmoType.toLowerCase() + "_" + mmoId.toLowerCase() + "_none";
        } else {
            ruleName = material.name().toLowerCase() + "_none";
        }

        customRules.removeIf(r -> {
            if (mmoType != null && mmoId != null) {
                return mmoType.equalsIgnoreCase(r.mmoItemType) && mmoId.equalsIgnoreCase(r.mmoItemId);
            }
            return r.material == material && r.mmoItemType == null;
        });

        customRules.add(new CustomRule(ruleName, material, null, null, true, mmoType, mmoId));

        if (raritiesConfig.getConfigurationSection("custom") == null) {
            raritiesConfig.createSection("custom");
        }

        String path = "custom." + ruleName;
        raritiesConfig.set(path + ".material", material.name());
        raritiesConfig.set(path + ".no-rarity", true);
        raritiesConfig.set(path + ".rarity", null);
        raritiesConfig.set(path + ".lore", null);
        raritiesConfig.set(path + ".mmo-type", mmoType);
        raritiesConfig.set(path + ".mmo-id", mmoId);

        saveRarities();
    }

    public void addNoRarityRule(Material material) {
        addNoRarityRule(material, null, null);
    }

    // ... saveRarities ...

    // ... getters ...

    // Keep the overload for backward compatibility/simplicity if needed,
    // but the main command should call the one with lore.
    public void addCustomRule(Material material, String rarityId) {
        addCustomRule(material, rarityId, new ArrayList<>());
    }

    public boolean removeCustomRule(Material material) {
        // Find rule to remove
        CustomRule toRemove = null;
        for (CustomRule rule : customRules) {
            if (rule.material == material) {
                toRemove = rule;
                break;
            }
        }

        if (toRemove == null)
            return false;

        // Remove from memory
        customRules.remove(toRemove);

        // Remove from file
        if (raritiesConfig.getConfigurationSection("custom") != null) {
            raritiesConfig.set("custom." + toRemove.id, null);
        }

        saveRarities();
        return true;
    }

    private void saveRarities() {
        try {
            raritiesConfig.save(raritiesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save rarities.yml", e);
        }
    }

    public RarityDefinition getRarity(String identifier) {
        return rarities.get(identifier);
    }

    public Collection<RarityDefinition> getRarities() {
        return rarities.values();
    }

    public List<CustomRule> getCustomRules() {
        return customRules;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public List<String> getLoreFormat() {
        return loreFormat;
    }

    // Data Classes
    public static class RarityDefinition {
        public final String identifier;
        public final String displayName;
        public final int priority;

        public RarityDefinition(String identifier, String displayName, int priority) {
            this.identifier = identifier;
            this.displayName = displayName;
            this.priority = priority;
        }
    }

    public static class CustomRule {
        public final String id;
        public final Material material;
        public final String targetRarityId;
        public final List<String> lore;
        public final boolean noRarity;
        public final String mmoItemType;
        public final String mmoItemId;

        public CustomRule(String id, Material material, String targetRarityId, List<String> lore, boolean noRarity,
                String mmoItemType, String mmoItemId) {
            this.id = id;
            this.material = material;
            this.targetRarityId = targetRarityId;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.noRarity = noRarity;
            this.mmoItemType = mmoItemType;
            this.mmoItemId = mmoItemId;
        }

        public CustomRule(String id, Material material, String targetRarityId, List<String> lore, boolean noRarity) {
            this(id, material, targetRarityId, lore, noRarity, null, null);
        }

        public CustomRule(String id, Material material, String targetRarityId, List<String> lore) {
            this(id, material, targetRarityId, lore, false, null, null);
        }

        public CustomRule(String id, Material material, String targetRarityId) {
            this(id, material, targetRarityId, new ArrayList<>(), false, null, null);
        }
    }
}
