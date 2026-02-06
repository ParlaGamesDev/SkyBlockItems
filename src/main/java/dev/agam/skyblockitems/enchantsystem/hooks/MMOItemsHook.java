package dev.agam.skyblockitems.enchantsystem.hooks;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

/**
 * Hook for MMOItems integration.
 * Provides NBT manipulation for custom stats.
 */
public class MMOItemsHook {

    private final SkyBlockItems plugin;

    public MMOItemsHook(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Set a stat value on an item using MMOItems NBT system.
     * 
     * @param item  The item to modify
     * @param stat  The stat name (without MMOITEMS_ prefix)
     * @param value The value to set
     * @return The modified item
     */
    public ItemStack setStat(ItemStack item, String stat, String value) {
        if (item == null)
            return item;

        NBTItem nbt = NBTItem.get(item);
        String tag = "MMOITEMS_" + stat;
        nbt.addTag(new ItemTag(tag, value));
        return nbt.toItem();
    }

    /**
     * Set a numeric stat value on an item.
     */
    public ItemStack setStat(ItemStack item, String stat, double value) {
        return setStat(item, stat, String.valueOf(value));
    }

    /**
     * Get a stat value from an item.
     */
    public String getStat(ItemStack item, String stat) {
        if (item == null)
            return null;

        NBTItem nbt = NBTItem.get(item);
        String tag = "MMOITEMS_" + stat;
        if (nbt.hasTag(tag)) {
            return nbt.getString(tag);
        }
        return null;
    }

    /**
     * Check if an item has a specific stat.
     */
    public boolean hasStat(ItemStack item, String stat) {
        if (item == null)
            return false;

        NBTItem nbt = NBTItem.get(item);
        return nbt.hasTag("MMOITEMS_" + stat);
    }

    /**
     * Get a numeric stat value from an item.
     */
    public double getNumericStat(ItemStack item, String stat, double defaultValue) {
        String value = getStat(item, stat);
        if (value == null)
            return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get the rarity of an item.
     */
    public String getRarity(ItemStack item) {
        String rarity = getStat(item, "RARITY");
        return rarity != null ? rarity : "COMMON";
    }

    public String getType(ItemStack item) {
        String type = getStat(item, "TYPE");
        return type != null ? type : item.getType().name();
    }
}
