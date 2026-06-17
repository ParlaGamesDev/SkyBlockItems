package dev.agam.skyblockitems.crafting;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Base class for all custom recipes.
 */
public abstract class SkyBlockRecipe {

    protected final ItemStack result;
    private String craftPermission;

    public SkyBlockRecipe(ItemStack result) {
        this.result = result;
    }

    public ItemStack getResult() {
        return result.clone();
    }

    public String getCraftPermission() {
        return craftPermission;
    }

    public void setCraftPermission(String craftPermission) {
        this.craftPermission = (craftPermission == null || craftPermission.isBlank()) ? null : craftPermission.trim();
    }

    public boolean requiresCraftPermission() {
        return craftPermission != null && !craftPermission.isBlank();
    }

    public abstract boolean matches(ItemStack[] matrix);
    
    public abstract void consume(ItemStack[] matrix);
}
