package dev.agam.skyblockitems.rarity;

/**
 * Represents a single item rarity.
 */
public class Rarity {

    private final String identifier;
    private final String displayName;
    private final int weight;
    private final int priority;
    private final boolean isDefault;

    public Rarity(String identifier, String displayName, int weight, int priority, boolean isDefault) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.weight = weight;
        this.priority = priority;
        this.isDefault = isDefault;
    }

    /**
     * Gets the unique identifier for this rarity (e.g., "Common", "Rare").
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the colored display name for this rarity.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the weight of this rarity (higher = rarer).
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Gets the priority/order of this rarity in config.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Returns true if this is the default rarity for unspecified items.
     */
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String toString() {
        return "Rarity{" +
                "identifier='" + identifier + '\'' +
                ", weight=" + weight +
                ", isDefault=" + isDefault +
                '}';
    }
}
