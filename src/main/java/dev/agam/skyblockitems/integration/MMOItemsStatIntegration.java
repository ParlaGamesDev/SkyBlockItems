package dev.agam.skyblockitems.integration;

import io.lumine.mythic.lib.player.modifier.ModifierType;
import io.lumine.mythic.lib.player.modifier.ModifierSource;
import io.lumine.mythic.lib.api.stat.modifier.StatModifier;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.StatHistory;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;

/**
 * Helper class for integrating reforge stats with MMOItems' native stat system.
 * This ensures proper Hebrew translation, icons, and (+X) bonus display from
 * language/stats.yml.
 */
public class MMOItemsStatIntegration {

    /**
     * Checks if an ItemStack is an MMOItem by looking for MMOItems NBT tags.
     * 
     * @param item The item to check
     * @return true if this is an MMOItem, false otherwise
     */
    public static boolean isMMOItem(ItemStack item) {
        if (item == null)
            return false;
        NBTItem nbtItem = NBTItem.get(item);
        return nbtItem.hasTag("MMOITEMS_ITEM_ID");
    }

    /**
     * Applies a stat bonus to an MMOItem using direct NBT manipulation.
     * This avoids rebuilding the item from template, preserving all custom metadata
     * like reforges, rarity lore, and custom names.
     * 
     * @param item   The MMOItem to modify
     * @param statId The stat identifier (e.g., "attack_damage")
     * @param value  The stat value to add
     * @return The modified ItemStack
     */
    public static ItemStack applyMMOItemStat(ItemStack item, String statId, double value) {
        if (!isMMOItem(item))
            return item;

        try {
            NBTItem nbtItem = NBTItem.get(item);

            // Convert stat ID format for NBT: "attack_damage" -> "MMOITEMS_ATTACK_DAMAGE"
            String statKey = "MMOITEMS_" + statId.toUpperCase().replace("-", "_");

            // Get existing base value if any
            double currentBase = nbtItem.getDouble(statKey);

            // Apply value directly to NBT.
            // Note: MMOItems stores base stats in these keys.
            nbtItem.addTag(new io.lumine.mythic.lib.api.item.ItemTag(statKey, currentBase + value));

            return nbtItem.toItem();
        } catch (Exception e) {
            e.printStackTrace();
            return item;
        }
    }

    /**
     * Removes reforge stat bonuses from an MMOItem using direct NBT.
     */
    public static ItemStack removeReforgeStats(ItemStack item, Map<String, Double> reforgeStats) {
        if (!isMMOItem(item) || reforgeStats == null || reforgeStats.isEmpty()) {
            return item;
        }

        try {
            NBTItem nbtItem = NBTItem.get(item);

            for (Map.Entry<String, Double> entry : reforgeStats.entrySet()) {
                String statId = entry.getKey();
                double valueToRemove = entry.getValue();

                if (!statId.startsWith("mmoitems_"))
                    continue;

                String cleanStatId = statId.substring("mmoitems_".length());
                String statKey = "MMOITEMS_" + cleanStatId.toUpperCase().replace("-", "_");

                if (nbtItem.hasTag(statKey)) {
                    double currentValue = nbtItem.getDouble(statKey);
                    double newValue = Math.max(0, currentValue - valueToRemove);

                    if (newValue == 0) {
                        // We don't remove tags for base stats usually, but for reforges it's safer to
                        // just set to 0
                        // or previous base if we had it. For now, simple subtraction.
                        nbtItem.addTag(new io.lumine.mythic.lib.api.item.ItemTag(statKey, 0.0));
                    } else {
                        nbtItem.addTag(new io.lumine.mythic.lib.api.item.ItemTag(statKey, newValue));
                    }
                }
            }

            return nbtItem.toItem();
        } catch (Exception e) {
            e.printStackTrace();
            return item;
        }
    }

    /**
     * Gets the MMOItem type from an ItemStack.
     * 
     * @param item The item to check
     * @return The MMOItem type string, or null if not an MMOItem
     */
    public static String getMMOItemType(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbtItem = NBTItem.get(item);
        return nbtItem.hasTag("MMOITEMS_ITEM_TYPE") ? nbtItem.getString("MMOITEMS_ITEM_TYPE") : null;
    }

    /**
     * Gets the MMOItem ID from an ItemStack.
     * 
     * @param item The item to check
     * @return The MMOItem ID string, or null if not an MMOItem
     */
    public static String getMMOItemID(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbtItem = NBTItem.get(item);
        return nbtItem.hasTag("MMOITEMS_ITEM_ID") ? nbtItem.getString("MMOITEMS_ITEM_ID") : null;
    }
}
