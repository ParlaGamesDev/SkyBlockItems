package dev.agam.skyblockitems.enchantsystem;

import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.LevelConfig;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Material;
import java.util.*;

/**
 * Represents a custom enchantment created through MMOEnchants.
 */
public class CustomEnchant {

    private final String id;
    private String displayName;
    private String description;
    private Material material;
    private List<String> targets;
    private int maxLevel;
    private final Map<EnchantStat, Double> stats = new HashMap<>(); // stat -> value per level
    private final Map<Integer, Integer> xpCosts = new HashMap<>(); // level -> xp cost
    private List<String> conflicts = new ArrayList<>();
    private int requiredEnchantingLevel = 0; // Required AuraSkills enchanting level
    private int anvilMultiplier = 2;
    private boolean enabled = true;

    public CustomEnchant(String id) {
        this.id = id;
        this.displayName = "&f" + id;
        this.description = "&7No description";
        this.material = Material.ENCHANTED_BOOK;
        this.targets = new ArrayList<>(List.of("SWORD"));
        this.maxLevel = 5;
        // this.stats = new HashMap<>(); // Initialized directly
        // this.xpCosts = new HashMap<>(); // Initialized directly

        // Default XP costs
        for (int i = 1; i <= 10; i++) {
            xpCosts.put(i, i * 10);
        }
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getMaterial() {
        return material;
    }

    public List<String> getTargets() {
        return targets;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Map<EnchantStat, Double> getStats() {
        return stats;
    }

    public Map<Integer, Integer> getXpCosts() {
        return xpCosts;
    }

    public int getXpCost(int level) {
        return xpCosts.getOrDefault(level, level * 10);
    }

    public double getStatValue(EnchantStat stat, int level) {
        Double perLevel = stats.get(stat);
        if (perLevel == null)
            return 0;
        return perLevel * level;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Setters
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public int getRequiredEnchantingLevel() {
        return requiredEnchantingLevel;
    }

    public void setRequiredEnchantingLevel(int level) {
        this.requiredEnchantingLevel = Math.max(0, level);
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = Math.min(10, Math.max(1, maxLevel));
    }

    public void setStat(EnchantStat stat, double valuePerLevel) {
        if (valuePerLevel <= 0) {
            stats.remove(stat);
        } else {
            stats.put(stat, valuePerLevel);
        }
    }

    public void setXpCost(int level, int cost) {
        xpCosts.put(level, cost);
    }

    /**
     * Converts this custom enchant to an EnchantConfig for use in the GUI.
     */
    public EnchantConfig toEnchantConfig() {
        Map<Integer, LevelConfig> levelConfigs = new HashMap<>();
        for (int i = 1; i <= maxLevel; i++) {
            levelConfigs.put(i, new LevelConfig(String.valueOf(i), getXpCost(i)));
        }

        return new EnchantConfig(
                id, displayName, description, material, targets,
                null, null, null, null, "HAND", 0, enabled, levelConfigs, conflicts, requiredEnchantingLevel,
                anvilMultiplier);
    }

    public int getAnvilMultiplier() {
        return anvilMultiplier;
    }

    public void setAnvilMultiplier(int anvilMultiplier) {
        this.anvilMultiplier = anvilMultiplier;
    }

    /**
     * Available stat types for custom enchantments
     */
    public enum EnchantStat {
        NONE("NONE", true),
        MAX_HEALTH("MAX_HEALTH", false),
        ATTACK_DAMAGE("ATTACK_DAMAGE", false),
        MOVEMENT_SPEED("MOVEMENT_SPEED", false),
        ARMOR("ARMOR", false),
        LUCK("LUCK", false),
        CRITICAL_STRIKE_CHANCE("CRITICAL_STRIKE_CHANCE", false),
        CRITICAL_STRIKE_DAMAGE("CRITICAL_STRIKE_DAMAGE", false),
        BLOCK_CHANCE("BLOCK_CHANCE", false),
        DODGE_CHANCE("DODGE_CHANCE", false),
        PARRY_CHANCE("PARRY_CHANCE", false),
        XP_BOOST("XP_BOOST", false),
        FISHING_REEL_SPEED("FISHING_REEL_SPEED", false),
        KNOCKBACK_RESISTANCE("KNOCKBACK_RESISTANCE", false);

        private final String mmoItemsId;
        private final boolean isBoolean;

        EnchantStat(String mmoItemsId, boolean isBoolean) {
            this.mmoItemsId = mmoItemsId;
            this.isBoolean = isBoolean;
        }

        public String getDisplayName() {
            return dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfigManager()
                    .getMessageRaw("stats." + name().toLowerCase() + ".name");
        }

        public String getLoreFormat() {
            return dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfigManager()
                    .getMessageRaw("stats." + name().toLowerCase() + ".lore-format");
        }

        public String getHebrewName() {
            return dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfigManager()
                    .getMessageRaw("stats." + name().toLowerCase() + ".hebrew-name");
        }

        public String getDescription() {
            return dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfigManager()
                    .getMessageRaw("stats." + name().toLowerCase() + ".description");
        }

        public String getMmoItemsId() {
            return mmoItemsId;
        }

        public boolean isBoolean() {
            return isBoolean;
        }

        public String formatLore(double value) {
            String format = getLoreFormat();
            if (isBoolean) {
                return ColorUtils.colorize(format);
            }
            return ColorUtils.colorize(format.replace("{value}", String.format("%.1f", value)));
        }
    }
}
