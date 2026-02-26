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
    private final Map<String, RarityData> rarityDataMap;
    private final ReforgeGem gem;

    /**
     * Represents data specific to a rarity tier.
     */
    public static class RarityData {
        private final double cost;
        private final Map<String, Double> stats;
        private final List<String> enchants;

        public RarityData(double cost, Map<String, Double> stats, List<String> enchants) {
            this.cost = cost;
            this.stats = stats != null ? stats : new HashMap<>();
            this.enchants = enchants != null ? enchants : new ArrayList<>();
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
    }

    /**
     * Constructs a Reforge from a ConfigurationSection.
     */
    public Reforge(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = ColorUtils.colorize(section.getString("display-name", id));
        this.itemTypes = section.getStringList("item-types");
        this.rarityDataMap = new HashMap<>();

        ConfigurationSection raritiesSection = section.getConfigurationSection("rarities");
        if (raritiesSection != null) {
            for (String rarityKey : raritiesSection.getKeys(false)) {
                ConfigurationSection rs = raritiesSection.getConfigurationSection(rarityKey);
                if (rs != null) {
                    double cost = rs.getDouble("cost", 0.0);
                    Map<String, Double> stats = new HashMap<>();
                    ConfigurationSection ss = rs.getConfigurationSection("stats");
                    if (ss != null) {
                        for (String sk : ss.getKeys(false))
                            stats.put(sk, ss.getDouble(sk));
                    }
                    List<String> enchants = rs.getStringList("enchants");
                    rarityDataMap.put(rarityKey.toUpperCase(), new RarityData(cost, stats, enchants));
                }
            }
        } else {
            // Migration/Legacy support: Load old top-level fields into all standard
            // rarities
            double cost = section.getDouble("cost", 0.0);
            Map<String, Double> stats = new HashMap<>();
            ConfigurationSection ss = section.getConfigurationSection("stats");
            if (ss != null) {
                for (String sk : ss.getKeys(false))
                    stats.put(sk, ss.getDouble(sk));
            }
            List<String> enchants = section.getStringList("enchants");
            RarityData legacy = new RarityData(cost, stats, enchants);
            for (String r : Arrays.asList("COMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC", "DIVINE")) {
                rarityDataMap.put(r, legacy);
            }
        }

        // Load gem
        ConfigurationSection gemSection = section.getConfigurationSection("gem");
        this.gem = gemSection != null ? new ReforgeGem(gemSection) : null;
    }

    public RarityData getDataFor(String rarity) {
        if (rarity == null)
            return rarityDataMap.getOrDefault("COMMON", rarityDataMap.values().iterator().next());
        return rarityDataMap.getOrDefault(rarity.toUpperCase(),
                rarityDataMap.getOrDefault("COMMON", rarityDataMap.values().iterator().next()));
    }

    public Map<String, RarityData> getRarityDataMap() {
        return new HashMap<>(rarityDataMap);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getItemTypes() {
        return new ArrayList<>(itemTypes);
    }

    public ReforgeGem getGem() {
        return gem;
    }

    public boolean hasGem() {
        return gem != null;
    }

    public boolean isCompatibleWith(String itemType) {
        if (itemTypes.isEmpty() || itemTypes.contains("ALL") || itemTypes.contains("GLOBAL"))
            return true;

        String upperItemType = itemType.toUpperCase();
        for (String type : itemTypes) {
            String upperType = type.toUpperCase();
            if (upperType.equals(upperItemType))
                return true;

            // Category matching
            if (upperType.equals("ARMOR")) {
                if (upperItemType.contains("HELMET") || upperItemType.contains("CHESTPLATE") ||
                        upperItemType.contains("LEGGINGS") || upperItemType.contains("BOOTS") ||
                        upperItemType.equals("ARMOR")) {
                    return true;
                }
            }
            if (upperType.equals("MELEE") || upperType.equals("WEAPON")) {
                if (upperItemType.contains("SWORD") || upperItemType.contains("AXE")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Reforge{id=" + id + ", displayName=" + displayName + ", types=" + itemTypes + "}";
    }
}
