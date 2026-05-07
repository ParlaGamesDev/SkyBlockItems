package dev.agam.skyblockitems.crafting;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A custom shapeless recipe implementation.
 */
public class ShapelessSkyBlockRecipe extends SkyBlockRecipe {

    private final List<ItemStack> ingredients;

    public ShapelessSkyBlockRecipe(ItemStack result, List<ItemStack> ingredients) {
        super(result);
        this.ingredients = ingredients;
    }

    public List<ItemStack> getIngredients() {
        return ingredients;
    }

    @Override
    public boolean matches(ItemStack[] matrix) {
        List<ItemStack> matrixItems = new ArrayList<>();
        for (ItemStack is : matrix) {
            if (is != null && !is.getType().isAir()) matrixItems.add(is);
        }

        if (matrixItems.size() != ingredients.size()) return false;

        List<ItemStack> remainingIngredients = new ArrayList<>(ingredients);
        for (ItemStack input : matrixItems) {
            boolean found = false;
            for (int i = 0; i < remainingIngredients.size(); i++) {
                if (RecipeMatcher.matches(input, remainingIngredients.get(i))) {
                    remainingIngredients.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return remainingIngredients.isEmpty();
    }

    @Override
    public void consume(ItemStack[] matrix) {
        List<ItemStack> remainingIngredients = new ArrayList<>(ingredients);
        for (ItemStack input : matrix) {
            if (input == null || input.getType().isAir()) continue;
            for (int i = 0; i < remainingIngredients.size(); i++) {
                if (RecipeMatcher.matches(input, remainingIngredients.get(i))) {
                    input.setAmount(input.getAmount() - remainingIngredients.get(i).getAmount());
                    remainingIngredients.remove(i);
                    break;
                }
            }
        }
    }
}
