package dev.agam.skyblockitems.crafting;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Integer> reqCounts = buildRequirementCounts();

        for (Map.Entry<String, Integer> entry : reqCounts.entrySet()) {
            int found = 0;
            for (ItemStack is : matrix) {
                if (is != null && RecipeMatcher.matchesRequirementId(is, entry.getKey())) {
                    found += is.getAmount();
                }
            }
            if (found != entry.getValue()) {
                return false;
            }
        }

        for (ItemStack is : matrix) {
            if (is == null || is.getType() == org.bukkit.Material.AIR) {
                continue;
            }
            boolean matchesAny = false;
            for (String reqId : reqCounts.keySet()) {
                if (RecipeMatcher.matchesRequirementId(is, reqId)) {
                    matchesAny = true;
                    break;
                }
            }
            if (!matchesAny) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean consume(ItemStack[] matrix) {
        Map<String, Integer> reqCounts = buildRequirementCounts();

        for (Map.Entry<String, Integer> entry : reqCounts.entrySet()) {
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

        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null && matrix[i].getAmount() <= 0) {
                matrix[i] = null;
            }
        }
        return true;
    }

    private Map<String, Integer> buildRequirementCounts() {
        Map<String, Integer> reqCounts = new HashMap<>();
        for (ItemStack is : ingredients) {
            if (is == null || is.getType() == org.bukkit.Material.AIR) {
                continue;
            }
            String id = RecipeMatcher.getIdentifier(is);
            reqCounts.put(id, reqCounts.getOrDefault(id, 0) + is.getAmount());
        }
        return reqCounts;
    }
}
