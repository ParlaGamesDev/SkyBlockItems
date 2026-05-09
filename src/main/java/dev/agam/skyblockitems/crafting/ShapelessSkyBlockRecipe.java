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
        // Count all items in matrix
        java.util.Map<String, Integer> matrixCounts = new java.util.HashMap<>();
        for (ItemStack is : matrix) {
            if (is == null || is.getType() == org.bukkit.Material.AIR) continue;
            String id = RecipeMatcher.getIdentifier(is);
            matrixCounts.put(id, matrixCounts.getOrDefault(id, 0) + is.getAmount());
        }

        // Count all ingredients in recipe
        java.util.Map<String, Integer> reqCounts = new java.util.HashMap<>();
        for (ItemStack is : ingredients) {
            if (is == null || is.getType() == org.bukkit.Material.AIR) continue;
            String id = RecipeMatcher.getIdentifier(is);
            reqCounts.put(id, reqCounts.getOrDefault(id, 0) + is.getAmount());
        }

        // Must have at least as many unique types as required
        if (matrixCounts.size() != reqCounts.size()) return false;

        for (java.util.Map.Entry<String, Integer> entry : reqCounts.entrySet()) {
            if (!matrixCounts.containsKey(entry.getKey()) || matrixCounts.get(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void consume(ItemStack[] matrix) {
        java.util.Map<String, Integer> reqCounts = new java.util.HashMap<>();
        for (ItemStack is : ingredients) {
            if (is == null || is.getType() == org.bukkit.Material.AIR) continue;
            String id = RecipeMatcher.getIdentifier(is);
            reqCounts.put(id, reqCounts.getOrDefault(id, 0) + is.getAmount());
        }

        for (java.util.Map.Entry<String, Integer> entry : reqCounts.entrySet()) {
            int remaining = entry.getValue();
            for (ItemStack is : matrix) {
                if (is != null && RecipeMatcher.getIdentifier(is).equals(entry.getKey())) {
                    int take = Math.min(is.getAmount(), remaining);
                    is.setAmount(is.getAmount() - take);
                    remaining -= take;
                    if (remaining <= 0) break;
                }
            }
        }
    }
}
