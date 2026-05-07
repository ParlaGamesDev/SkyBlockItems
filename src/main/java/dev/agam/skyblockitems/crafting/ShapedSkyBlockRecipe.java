package dev.agam.skyblockitems.crafting;

import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * A custom shaped recipe implementation.
 */
public class ShapedSkyBlockRecipe extends SkyBlockRecipe {

    private final ItemStack[] ingredients; // 9 slots

    public ShapedSkyBlockRecipe(ItemStack result, ItemStack[] ingredients) {
        super(result);
        this.ingredients = ingredients;
    }

    public ItemStack[] getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(ItemStack[] matrix) {
        if (matrix.length != 9) return false;
        for (int i = 0; i < 9; i++) {
            if (!RecipeMatcher.matches(matrix[i], ingredients[i])) return false;
        }
        return true;
    }

    @Override
    public void consume(ItemStack[] matrix) {
        for (int i = 0; i < 9; i++) {
            ItemStack item = matrix[i];
            if (item != null && ingredients[i] != null) {
                item.setAmount(item.getAmount() - ingredients[i].getAmount());
            }
        }
    }
}
