package dev.agam.skyblockitems.crafting;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Manages custom and vanilla recipes with MMOItems support.
 */
public class CraftingManager {

    private final SkyBlockItems plugin;
    private final List<SkyBlockRecipe> customRecipes = new ArrayList<>();

    public CraftingManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        loadRecipes();
    }

    /**
     * Finds all recipes the player can craft from their inventory.
     * Priority: MMOItems recipes first, then Vanilla.
     */
    public List<ItemStack> getCraftableResults(org.bukkit.entity.Player player) {
        List<ItemStack> craftable = new ArrayList<>();
        Set<String> added = new HashSet<>();
        ItemStack[] contents = player.getInventory().getStorageContents();
        
        // 1. Check Custom/MMOItems Recipes
        for (SkyBlockRecipe recipe : customRecipes) {
            if (canCraft(contents, recipe)) {
                ItemStack res = recipe.getResult();
                String key = res.getType() + ":" + (res.hasItemMeta() && res.getItemMeta().hasDisplayName() ? res.getItemMeta().getDisplayName() : "");
                if (added.add(key)) {
                    craftable.add(res);
                }
            }
        }

        // Add common vanilla recipes (e.g., blocks, tools)
        Iterator<Recipe> it = Bukkit.recipeIterator();
        int count = 0;
        while (it.hasNext() && count < 20) { // Limit to avoid lag
            Recipe r = it.next();
            if (canCraftVanilla(player, r)) {
                ItemStack res = r.getResult();
                String key = "VANILLA:" + res.getType();
                if (added.add(key)) {
                    craftable.add(res);
                    count++;
                }
            }
        }
        
        return craftable;
    }

    private boolean canCraftVanilla(org.bukkit.entity.Player player, Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return hasAllIngredients(player.getInventory().getStorageContents(), shaped.getIngredientMap().values().toArray(new ItemStack[0]));
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            return hasAllIngredients(player.getInventory().getStorageContents(), shapeless.getIngredientList().toArray(new ItemStack[0]));
        }
        return false;
    }

    private boolean canCraft(ItemStack[] inventory, SkyBlockRecipe recipe) {
        if (recipe instanceof ShapedSkyBlockRecipe shaped) {
            // For shaped, we just need to ensure all ingredients are present in enough quantity
            // This is a simplified check for quick craft (shapeless style scanning of inventory)
            return hasAllIngredients(inventory, shaped.getIngredients());
        } else if (recipe instanceof ShapelessSkyBlockRecipe shapeless) {
            return hasAllIngredients(inventory, shapeless.getIngredients().toArray(new ItemStack[0]));
        }
        return false;
    }

    private boolean hasAllIngredients(ItemStack[] inventory, ItemStack[] ingredients) {
        List<ItemStack> tempInv = new ArrayList<>();
        for (ItemStack is : inventory) if (is != null) tempInv.add(is.clone());

        for (ItemStack req : ingredients) {
            if (req == null || req.getType() == Material.AIR) continue;
            int remaining = req.getAmount();
            for (ItemStack invItem : tempInv) {
                if (RecipeMatcher.matches(invItem, req)) {
                    int take = Math.min(invItem.getAmount(), remaining);
                    invItem.setAmount(invItem.getAmount() - take);
                    remaining -= take;
                    if (remaining <= 0) break;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    public Optional<SkyBlockRecipe> findMatchingRecipe(ItemStack[] matrix) {
        for (SkyBlockRecipe recipe : customRecipes) {
            if (recipe.matches(matrix)) return Optional.of(recipe);
        }
        return Optional.empty();
    }

    /**
     * Finds a matching recipe for the given 3x3 matrix.
     * Priority: Custom Recipes -> Vanilla Recipes.
     */
    public Optional<ItemStack> findResult(ItemStack[] matrix) {
        if (isMatrixEmpty(matrix)) return Optional.empty();

        // 1. Check Custom Recipes
        for (SkyBlockRecipe recipe : customRecipes) {
            if (recipe.matches(matrix)) {
                return Optional.of(recipe.getResult());
            }
        }

        // 2. Check Vanilla Recipes
        Recipe vanilla = Bukkit.getCraftingRecipe(matrix, Bukkit.getWorlds().get(0));
        if (vanilla != null) {
            // Check if ingredients contain restricted MMOItems
            if (containsRestrictedItems(matrix)) return Optional.empty();
            return Optional.of(vanilla.getResult().clone());
        }

        return Optional.empty();
    }

    private boolean isMatrixEmpty(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) return false;
        }
        return true;
    }

    private boolean containsRestrictedItems(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (RecipeMatcher.isRestricted(item)) return true;
        }
        return false;
    }

    public void registerRecipe(SkyBlockRecipe recipe) {
        customRecipes.add(recipe);
    }

    public List<SkyBlockRecipe> getCustomRecipes() {
        return customRecipes;
    }

    public void consumeFromInventory(org.bukkit.entity.Player player, SkyBlockRecipe recipe) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        ItemStack[] ingredients = (recipe instanceof ShapedSkyBlockRecipe s) ? s.getIngredients() : 
                                   ((ShapelessSkyBlockRecipe)recipe).getIngredients().toArray(new ItemStack[0]);

        for (ItemStack req : ingredients) {
            if (req == null || req.getType() == Material.AIR) continue;
            int remaining = req.getAmount();
            for (ItemStack invItem : contents) {
                if (invItem != null && RecipeMatcher.matches(invItem, req)) {
                    int take = Math.min(invItem.getAmount(), remaining);
                    invItem.setAmount(invItem.getAmount() - take);
                    remaining -= take;
                    if (remaining <= 0) break;
                }
            }
        }
        player.getInventory().setStorageContents(contents);
    }

    public void loadRecipes() {
        customRecipes.clear();
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("recipes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    org.bukkit.configuration.ConfigurationSection recipeSec = section.getConfigurationSection(key);
                    if (recipeSec == null) continue;
                    ItemStack result = parseItem(recipeSec.getConfigurationSection("result"));
                    if (result == null) continue;
                    String type = recipeSec.getString("type", "SHAPED");
                    if (type.equalsIgnoreCase("SHAPED")) {
                        List<String> shape = recipeSec.getStringList("shape");
                        org.bukkit.configuration.ConfigurationSection ingSec = recipeSec.getConfigurationSection("ingredients");
                        ItemStack[] matrix = new ItemStack[9];
                        if (ingSec != null) {
                            for (int r = 0; r < shape.size(); r++) {
                                for (int c = 0; c < shape.get(r).length(); c++) {
                                    char symbol = shape.get(r).charAt(c);
                                    if (symbol != ' ') {
                                        matrix[r * 3 + c] = parseItem(ingSec.getConfigurationSection(String.valueOf(symbol)));
                                    }
                                }
                            }
                        }
                        registerRecipe(new ShapedSkyBlockRecipe(result, matrix));
                    } else {
                        List<ItemStack> ingredients = new ArrayList<>();
                        org.bukkit.configuration.ConfigurationSection ingSec = recipeSec.getConfigurationSection("ingredients");
                        if (ingSec != null) {
                            for (String iKey : ingSec.getKeys(false)) {
                                ingredients.add(parseItem(ingSec.getConfigurationSection(iKey)));
                            }
                        }
                        registerRecipe(new ShapelessSkyBlockRecipe(result, ingredients));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load recipe: " + key + " - " + e.getMessage());
                }
            }
        }

        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            loadMMOItemsRecipes();
        }
        plugin.getLogger().info("CraftingManager: Loaded " + customRecipes.size() + " custom recipes.");
    }

    private void loadMMOItemsRecipes() {
        plugin.getLogger().info("CraftingManager: Scanning Bukkit registry for MMOItems recipes...");
        Iterator<Recipe> it = Bukkit.recipeIterator();
        int count = 0;
        
        while (it.hasNext()) {
            Recipe recipe = it.next();
            ItemStack result = recipe.getResult();
            
            // Check if result is an MMOItem
            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(result);
            if (nbt.hasType()) {
                if (recipe instanceof ShapedRecipe shaped) {
                    // Convert Shaped to SBI Shaped
                    ItemStack[] matrix = new ItemStack[9];
                    String[] shape = shaped.getShape();
                    java.util.Map<Character, ItemStack> ingMap = shaped.getIngredientMap();
                    
                    for (int r = 0; r < shape.length; r++) {
                        for (int c = 0; c < shape[r].length(); c++) {
                            char symbol = shape[r].charAt(c);
                            matrix[r * 3 + c] = ingMap.get(symbol);
                        }
                    }
                    registerRecipe(new ShapedSkyBlockRecipe(result, matrix));
                    count++;
                } else if (recipe instanceof ShapelessRecipe shapeless) {
                    // Convert Shapeless to SBI Shapeless
                    registerRecipe(new ShapelessSkyBlockRecipe(result, shapeless.getIngredientList()));
                    count++;
                }
            }
        }
        plugin.getLogger().info("CraftingManager: Automatically imported " + count + " MMOItems recipes.");
    }

    private ItemStack parseItem(org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) return null;
        if (section.contains("mmoitem")) {
            String typeId = section.getString("mmoitem.type");
            String id = section.getString("mmoitem.id");
            net.Indyuce.mmoitems.api.Type type = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(typeId);
            if (type == null) return null;
            return net.Indyuce.mmoitems.MMOItems.plugin.getItem(type, id);
        }
        Material mat = Material.valueOf(section.getString("material", "AIR"));
        ItemStack item = new ItemStack(mat);
        item.setAmount(section.getInt("amount", 1));
        return item;
    }
}
