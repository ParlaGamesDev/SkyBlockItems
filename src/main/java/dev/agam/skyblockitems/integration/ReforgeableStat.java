package dev.agam.skyblockitems.integration;

import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom MMOItems stat that determines whether an item can be reforged.
 * By default, MMOItems CANNOT be reforged (false).
 * Setting this to true explicitly allows the item to be reforged.
 */
public class ReforgeableStat extends BooleanStat {

    public ReforgeableStat() {
        super("REFORGEABLE", Material.ANVIL, "ניתן לחישול",
                new String[] { "האם ניתן לבצע חישול לפריט זה?" },
                new String[] { "tool", "armor", "weapon", "accessory", "gem_stone", "consumable", "miscellaneous" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        // Store the boolean value in NBT
        item.addItemTag(new ItemTag("MMOITEMS_REFORGEABLE", data.isEnabled()));
    }

    /**
     * Checks if an ItemStack is reforgeable according to this stat.
     * Returns FALSE by default if the stat is not present - items must explicitly
     * opt-in.
     * 
     * @param item The item to check
     * @return true if the item can be reforged, false otherwise
     */
    public static boolean isReforgeable(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        NBTItem nbtItem = NBTItem.get(item);

        // Must be an MMOItem first
        if (!nbtItem.hasTag("MMOITEMS_ITEM_ID")) {
            return false;
        }

        // If the stat is not set, default to FALSE (must explicitly enable)
        if (!nbtItem.hasTag("MMOITEMS_REFORGEABLE")) {
            return false;
        }

        // Return the actual value
        return nbtItem.getBoolean("MMOITEMS_REFORGEABLE");
    }
}
