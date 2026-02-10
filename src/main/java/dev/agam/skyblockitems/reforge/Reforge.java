package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Represents a single reforge configuration.
 * Loaded from reforges.yml and applied to items via the reforge system.
 */
public class Reforge {

    private final String id;
    private final String displayName;
    private final List<String> itemTypes;
    private final String rarityRequirement;
    private final String rarityUpgrade;
    private final double cost;
    private final Map<String, Double> stats;
    private final List<String> enchants;
    private final List<String> abilities;

    /**
     * Constructs a Reforge from a ConfigurationSection.
     * 
     * @param id      The unique identifier for this reforge
     * @param section The YAML configuration section containing reforge data
     */
    public Reforge(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = ColorUtils.colorize(section.getString("display-name", id));
        this.itemTypes = section.getStringList("item-types");
        this.rarityRequirement = section.getString("rarity-requirement", "COMMON");
        this.rarityUpgrade = section.getString("rarity-upgrade", "COMMON");
        this.cost = section.getDouble("cost", 0.0);

        // Load stats map
        this.stats = new HashMap<>();
        ConfigurationSection statsSection = section.getConfigurationSection("stats");
        if (statsSection != null) {
            for (String statKey : statsSection.getKeys(false)) {
                stats.put(statKey, statsSection.getDouble(statKey));
            }
        }

        // Load enchants and abilities
        this.enchants = section.getStringList("enchants");
        this.abilities = section.getStringList("abilities");
    }

    /**
     * Checks if this reforge can be applied to the given item type.
     * 
     * @param itemType The item type to check (e.g., "SWORD", "PICKAXE")
     * @return true if this reforge is compatible with the item type
     */
    public boolean isCompatibleWith(String itemType) {
        if (itemTypes.isEmpty() || itemTypes.contains("ALL") || itemTypes.contains("GLOBAL")) {
            return true;
        }
        return itemTypes.stream().anyMatch(type -> type.equalsIgnoreCase(itemType));
    }

    /**
     * Checks if the item's current rarity meets the requirement for this reforge.
     * 
     * @param currentRarity The current rarity identifier of the item
     * @return true if the requirement is met
     */
    public boolean meetsRarityRequirement(String currentRarity) {
        int currentWeight = getRarityWeight(currentRarity);
        int requiredWeight = getRarityWeight(rarityRequirement);
        return currentWeight >= requiredWeight;
    }

    /**
     * Gets the weight of a rarity tier for comparison.
     * Higher weight = rarer tier.
     */
    private int getRarityWeight(String rarity) {
        return switch (rarity.toUpperCase()) {
            case "COMMON" -> 1;
            case "UNCOMMON" -> 2;
            case "RARE" -> 3;
            case "EPIC" -> 4;
            case "LEGENDARY" -> 5;
            case "MYTHIC" -> 6;
            default -> 1;
        };
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getItemTypes() {
        return new ArrayList<>(itemTypes);
    }

    public String getRarityRequirement() {
        return rarityRequirement;
    }

    public String getRarityUpgrade() {
        return rarityUpgrade;
    }

    public double getCost() {
        return cost;
    }

    public Map<String, Double> getStats() {
        return new HashMap<>(stats);
    }

    public List<String> getEnchants() {
        return new ArrayList<>(enchants);
    }

    public List<String> getAbilities() {
        return new ArrayList<>(abilities);
    }

    @Override
    public String toString() {
        return "Reforge{id=" + id + ", displayName=" + displayName + ", types=" + itemTypes + "}";
    }
}
