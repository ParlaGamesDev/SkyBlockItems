package dev.agam.skyblockitems.crafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;

/**
 * A super-flexible recipe that only cares about total counts in the grid.
 */
public class FlexibleSkyBlockRecipe extends SkyBlockRecipe {

    private final Map<String, Integer> ingredients; // Identifier -> Required Amount

    public FlexibleSkyBlockRecipe(ItemStack result, Map<String, Integer> ingredients) {
        super(result);
        this.ingredients = ingredients;
    }

    public Map<String, Integer> getIngredientsMap() {
        return ingredients;
    }

    @Override
    public boolean matches(ItemStack[] matrix) {
        Map<String, Integer> matrixCounts = new HashMap<>();
        for (ItemStack is : matrix) {
            if (is == null || is.getType() == Material.AIR) continue;
            String id = RecipeMatcher.getIdentifier(is);
            matrixCounts.put(id, matrixCounts.getOrDefault(id, 0) + is.getAmount());
        }

        // If number of distinct items doesn't match, it's not our recipe (prevent overlap)
        if (matrixCounts.size() != ingredients.size()) return false;

        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            Integer current = matrixCounts.get(entry.getKey());
            if (current == null || !current.equals(entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public boolean consume(ItemStack[] matrix) {
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            int remaining = entry.getValue();
            for (ItemStack is : matrix) {
                if (is != null && RecipeMatcher.getIdentifier(is).equals(entry.getKey())) {
                    int take = Math.min(is.getAmount(), remaining);
                    is.setAmount(is.getAmount() - take);
                    remaining -= take;
                    if (remaining <= 0) break;
                }
            }
            if (remaining > 0)
                return false;
        }
        clearEmptyStacks(matrix);
        return true;
    }

    private void clearEmptyStacks(ItemStack[] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null && matrix[i].getAmount() <= 0)
                matrix[i] = null;
        }
    }
}
