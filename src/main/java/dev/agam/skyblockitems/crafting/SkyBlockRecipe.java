package dev.agam.skyblockitems.crafting;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Base class for all custom recipes.
 */
public abstract class SkyBlockRecipe {

    protected final ItemStack result;

    public SkyBlockRecipe(ItemStack result) {
        this.result = result;
    }

    public ItemStack getResult() {
        return result.clone();
    }

    public abstract boolean matches(ItemStack[] matrix);
    
    public abstract void consume(ItemStack[] matrix);
}
