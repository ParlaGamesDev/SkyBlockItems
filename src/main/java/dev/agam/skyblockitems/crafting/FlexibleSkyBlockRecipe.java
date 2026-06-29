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
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            int found = countMatchingInMatrix(matrix, entry.getKey());
            if (found != entry.getValue()) {
                return false;
            }
        }

        for (ItemStack is : matrix) {
            if (is == null || is.getType() == Material.AIR) {
                continue;
            }
            if (!matchesAnyRequirement(is)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean consume(ItemStack[] matrix) {
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            int remaining = entry.getValue();
            for (ItemStack is : matrix) {
                if (is != null && RecipeMatcher.matchesRequirementId(is, entry.getKey())) {
                    int take = Math.min(is.getAmount(), remaining);
                    is.setAmount(is.getAmount() - take);
                    remaining -= take;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        clearEmptyStacks(matrix);
        return true;
    }

    private int countMatchingInMatrix(ItemStack[] matrix, String requirementId) {
        int found = 0;
        for (ItemStack is : matrix) {
            if (is != null && RecipeMatcher.matchesRequirementId(is, requirementId)) {
                found += is.getAmount();
            }
        }
        return found;
    }

    private boolean matchesAnyRequirement(ItemStack item) {
        for (String requirementId : ingredients.keySet()) {
            if (RecipeMatcher.matchesRequirementId(item, requirementId)) {
                return true;
            }
        }
        return false;
    }

    private void clearEmptyStacks(ItemStack[] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null && matrix[i].getAmount() <= 0) {
                matrix[i] = null;
            }
        }
    }
}
