package dev.agam.skyblockitems.crafting;

import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Utility for comparing items including MMOItems NBT support.
 */
public class RecipeMatcher {

    /**
     * Checks if two items match. 
     * Supports: Material match, MMOItem Type/ID match.
     */
    public static boolean matches(ItemStack input, ItemStack ingredient) {
        if (input == null || ingredient == null) {
            return input == ingredient; // Both null matches
        }

        if (input.getType() != ingredient.getType()) return false;
        
        // If it's a basic material without special data, material match is enough
        if (!ingredient.hasItemMeta() && !input.hasItemMeta()) return true;

        // Check for MMOItems NBT
        NBTItem inputNbt = NBTItem.get(input);
        NBTItem ingNbt = NBTItem.get(ingredient);

        String inputType = getMMOType(inputNbt);
        String ingType = getMMOType(ingNbt);

        if (ingType != null) {
            if (!ingType.equalsIgnoreCase(inputType)) return false;
            String inputId = getMMOId(inputNbt);
            String ingId = getMMOId(ingNbt);
            return ingId != null && ingId.equalsIgnoreCase(inputId);
        }

        // If not an MMOItem, check material and ensure it's not an MMOItem at all
        if (inputType != null) return false; 

        // Vanilla comparison - ignore durability and other volatile tags if possible
        return input.getType() == ingredient.getType();
    }

    private static String getMMOType(NBTItem nbt) {
        if (nbt.hasType()) return nbt.getType();
        if (nbt.hasTag("MMOITEMS_ITEM_TYPE")) return nbt.getString("MMOITEMS_ITEM_TYPE");
        if (nbt.hasTag("mmoitems_item_type")) return nbt.getString("mmoitems_item_type");
        return null;
    }

    private static String getMMOId(NBTItem nbt) {
        if (nbt.hasTag("MMOITEMS_ITEM_ID")) return nbt.getString("MMOITEMS_ITEM_ID");
        if (nbt.hasTag("mmoitems_item_id")) return nbt.getString("mmoitems_item_id");
        return null;
    }

    public static boolean isRestricted(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbt = NBTItem.get(item);
        if (nbt.hasTag("MMOITEMS_DISABLE_CRAFTING")) return nbt.getBoolean("MMOITEMS_DISABLE_CRAFTING");
        if (nbt.hasTag("mmoitems_disable_crafting")) return nbt.getBoolean("mmoitems_disable_crafting");
        return false;
    }
}
