package dev.agam.skyblockitems.crafting;

import org.bukkit.Material;
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
        for (int yOff = -2; yOff <= 2; yOff++) {
            for (int xOff = -2; xOff <= 2; xOff++) {
                if (checkMatch(matrix, xOff, yOff)) return true;
            }
        }
        return false;
    }

    private boolean checkMatch(ItemStack[] matrix, int xOff, int yOff) {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int mR = r + yOff;
                int mC = c + xOff;
                ItemStack recipeIng = ingredients[r * 3 + c];
                ItemStack matrixItem = null;
                if (mR >= 0 && mR < 3 && mC >= 0 && mC < 3) {
                    matrixItem = matrix[mR * 3 + mC];
                }
                
                // If recipe expects an item here, check if matrix has it AND has enough amount
                if (recipeIng != null && recipeIng.getType() != Material.AIR) {
                    if (matrixItem == null || !RecipeMatcher.matches(matrixItem, recipeIng)) return false;
                    if (matrixItem.getAmount() < recipeIng.getAmount()) return false;
                } else {
                    // Recipe expects AIR here
                    if (matrixItem != null && matrixItem.getType() != Material.AIR) return false;
                }
            }
        }
        // Rest of the empty space check
        for (int i = 0; i < 9; i++) {
            boolean inRegion = false;
            int mR = i / 3;
            int mC = i % 3;
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    if (r + yOff == mR && c + xOff == mC) {
                        inRegion = true;
                        break;
                    }
                }
                if (inRegion) break;
            }
            if (!inRegion && matrix[i] != null && matrix[i].getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean consume(ItemStack[] matrix) {
        for (int yOff = -2; yOff <= 2; yOff++) {
            for (int xOff = -2; xOff <= 2; xOff++) {
                if (checkMatch(matrix, xOff, yOff)) {
                    for (int r = 0; r < 3; r++) {
                        for (int c = 0; c < 3; c++) {
                            int mR = r + yOff;
                            int mC = c + xOff;
                            if (mR >= 0 && mR < 3 && mC >= 0 && mC < 3) {
                                ItemStack item = matrix[mR * 3 + mC];
                                ItemStack recipeIng = ingredients[r * 3 + c];
                                if (item != null && recipeIng != null && recipeIng.getType() != Material.AIR) {
                                    item.setAmount(item.getAmount() - recipeIng.getAmount());
                                    if (item.getAmount() <= 0)
                                        matrix[mR * 3 + mC] = null;
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
