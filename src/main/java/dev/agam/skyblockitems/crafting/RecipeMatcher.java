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
     * Supports: Material match, MMOItem Type/ID match, vanilla log/wood families.
     */
    public static boolean matches(ItemStack input, ItemStack ingredient) {
        if (input == null || ingredient == null) {
            return input == ingredient; // Both null matches
        }

        if (input.getType() == ingredient.getType()) {
            return matchesSameType(input, ingredient);
        }

        NBTItem inputNbt = NBTItem.get(input);
        NBTItem ingNbt = NBTItem.get(ingredient);
        if (getMMOType(inputNbt) != null || getMMOType(ingNbt) != null) {
            return false;
        }

        return matchesMaterialFamily(input.getType(), ingredient.getType());
    }

    private static boolean matchesSameType(ItemStack input, ItemStack ingredient) {
        if (!ingredient.hasItemMeta() && !input.hasItemMeta()) {
            return true;
        }

        NBTItem inputNbt = NBTItem.get(input);
        NBTItem ingNbt = NBTItem.get(ingredient);

        String inputType = getMMOType(inputNbt);
        String ingType = getMMOType(ingNbt);

        if (ingType != null) {
            if (!ingType.equalsIgnoreCase(inputType)) {
                return false;
            }
            String inputId = getMMOId(inputNbt);
            String ingId = getMMOId(ingNbt);
            return ingId != null && ingId.equalsIgnoreCase(inputId);
        }

        return inputType == null;
    }

    /**
     * Checks whether an item satisfies a recipe requirement identifier.
     */
    public static boolean matchesRequirementId(ItemStack input, String requirementId) {
        if (input == null || input.getType() == Material.AIR || requirementId == null) {
            return false;
        }
        if (getIdentifier(input).equals(requirementId)) {
            return true;
        }
        if (!requirementId.startsWith("VANILLA:")) {
            return false;
        }
        Material required = Material.matchMaterial(requirementId.substring("VANILLA:".length()));
        if (required == null) {
            return false;
        }
        return matchesMaterialFamily(input.getType(), required);
    }

    /**
     * Vanilla log/wood/stem/hyphae variants of the same tree type are interchangeable.
     */
    public static boolean matchesMaterialFamily(Material input, Material required) {
        String inputFamily = getWoodFamilyKey(input);
        String requiredFamily = getWoodFamilyKey(required);
        return inputFamily != null && inputFamily.equals(requiredFamily);
    }

    public static String getWoodFamilyKey(Material material) {
        if (material == null) {
            return null;
        }
        String name = material.name();
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
        }
        for (String suffix : new String[]{"_LOG", "_WOOD", "_STEM", "_HYPHAE"}) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return null;
    }

    public static String getMMOType(NBTItem nbt) {
        if (nbt.hasType()) return nbt.getType();
        if (nbt.hasTag("MMOITEMS_ITEM_TYPE")) return nbt.getString("MMOITEMS_ITEM_TYPE");
        if (nbt.hasTag("mmoitems_item_type")) return nbt.getString("mmoitems_item_type");
        return null;
    }

    public static String getMMOId(NBTItem nbt) {
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

    public static String getIdentifier(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "AIR";
        NBTItem nbt = NBTItem.get(item);
        String type = getMMOType(nbt);
        String id = getMMOId(nbt);
        if (type != null && id != null) {
            return "MMO:" + type.toUpperCase() + ":" + id.toUpperCase();
        }
        return "VANILLA:" + item.getType().name();
    }
}
