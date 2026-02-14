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

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        NONE("NONE", true, "ללא", "ללא", "אין סטטיסטיקה מוגדרת.", "&7ללא"),
        MAX_HEALTH("MAX_HEALTH", false, "Max Health", "חיים מקסימליים", "מעלה את כמות החיים המקסימלית של השחקן.",
                "&7חיים מקסימליים: &a+{value}"),
        ATTACK_DAMAGE("ATTACK_DAMAGE", false, "Attack Damage", "נזק התקפה", "מעלה את הנזק הנגרם לאויבים.",
                "&7נזק התקפה: &a+{value}"),
        MOVEMENT_SPEED("MOVEMENT_SPEED", false, "Movement Speed", "מהירות תנועה", "מעלה את מהירות הריצה וההליכה.",
                "&7מהירות תנועה: &a+{value}"),
        ARMOR("ARMOR", false, "Armor", "שריון", "מעלה את ההגנה מפני נזק פיזי.", "&7שריון: &a+{value}"),
        LUCK("LUCK", false, "Luck", "מזל", "משפר את הסיכוי לקבל שלל טוב יותר.", "&7מזל: &a+{value}"),
        CRITICAL_STRIKE_CHANCE("CRITICAL_STRIKE_CHANCE", false, "Critical Strike Chance", "סיכוי למכה קריטית",
                "מעלה את הסיכוי להנחית מכה קריטית.", "&7סיכוי לקריטי: &a+{value}%"),
        CRITICAL_STRIKE_DAMAGE("CRITICAL_STRIKE_DAMAGE", false, "Critical Strike Damage", "נזק מכה קריטית",
                "מעלה את הנזק שנגרם במכות קריטיות.", "&7נזק קריטי: &a+{value}%"),
        BLOCK_CHANCE("BLOCK_CHANCE", false, "Block Chance", "סיכוי לחסימה", "מעלה את הסיכוי לחסום התקפות.",
                "&7סיכוי לחסימה: &a+{value}%"),
        DODGE_CHANCE("DODGE_CHANCE", false, "Dodge Chance", "סיכוי להתחמקות", "מעלה את סיכוי להתחמק מהתקפות.",
                "&7סיכוי להתחמקות: &a+{value}%"),
        PARRY_CHANCE("PARRY_CHANCE", false, "Parry Chance", "סיכוי להדיפה", "מעלה את הסיכוי להדוף התקפה ולהחזיר נזק.",
                "&7סיכוי להדיפה: &a+{value}%"),
        XP_BOOST("XP_BOOST", false, "XP Boost", "בוסט לנסיון", "מעלה את כמות ה-XP המתקבלת מפעולות.",
                "&7XP Boost: &a+{value}%"),
        FISHING_REEL_SPEED("FISHING_REEL_SPEED", false, "Fishing Reel Speed", "מהירות משיכת חכה",
                "מעלה את המהירות שבה דגים נתפסים.", "&7מהירות דיג: &a+{value}"),
        KNOCKBACK_RESISTANCE("KNOCKBACK_RESISTANCE", false, "Knockback Resistance", "עמידות להדיפה",
                "מפחית את המרחק שאליו נהדפים כשחוטפים מכה.", "&7עמידות להדיפה: &a+{value}");

        private final String mmoItemsId;
        private final boolean isBoolean;
        private final String displayName;
        private final String hebrewName;
        private final String description;
        private final String loreFormat;

        EnchantStat(String mmoItemsId, boolean isBoolean, String displayName, String hebrewName, String description,
                String loreFormat) {
            this.mmoItemsId = mmoItemsId;
            this.isBoolean = isBoolean;
            this.displayName = displayName;
            this.hebrewName = hebrewName;
            this.description = description;
            this.loreFormat = loreFormat;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getLoreFormat() {
            return loreFormat;
        }

        public String getHebrewName() {
            return hebrewName;
        }

        public String getDescription() {
            return description;
        }

        public String getMmoItemsId() {
            return mmoItemsId;
        }

        public boolean isBoolean() {
            return isBoolean;
        }

        public String formatLore(double value) {
            if (isBoolean) {
                return ColorUtils.colorize(loreFormat);
            }
            return ColorUtils.colorize(loreFormat.replace("{value}", String.format("%.1f", value)));
        }
    }
}
