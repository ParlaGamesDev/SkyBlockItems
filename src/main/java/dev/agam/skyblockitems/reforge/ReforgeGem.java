package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a special gem required for certain reforges.
 */
public class ReforgeGem {
    private final String id;
    private final String name;
    private final List<String> lore;
    private final String material;
    private final int customModelData;

    public ReforgeGem(String id, String name, List<String> lore, String material, int customModelData) {
        this.id = id;
        this.name = ColorUtils.colorize(name);
        this.lore = ColorUtils.colorizeList(lore);
        this.material = material;
        this.customModelData = customModelData;
    }

    public ReforgeGem(ConfigurationSection section) {
        this.id = section.getName();
        this.name = ColorUtils.colorize(section.getString("name", id));
        this.lore = ColorUtils.colorizeList(section.getStringList("lore"));
        this.material = section.getString("material", "STONE");
        this.customModelData = section.getInt("custom-model-data", 0);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    public String getMaterial() {
        return material;
    }

    public int getCustomModelData() {
        return customModelData;
    }
}
