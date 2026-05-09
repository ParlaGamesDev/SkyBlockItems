package dev.agam.skyblockitems.crafting;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.*;

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
    public List<ItemStack> getCraftableResults(org.bukkit.entity.Player player, ItemStack[] extraItems) {
        List<ItemStack> craftable = new ArrayList<>();
        List<ItemStack> combined = new ArrayList<>();
        for (ItemStack is : player.getInventory().getStorageContents()) if (is != null) combined.add(is.clone());
        if (extraItems != null) {
            for (ItemStack is : extraItems) if (is != null) combined.add(is.clone());
        }
        
        ItemStack[] contents = combined.toArray(new ItemStack[0]);
        Set<String> added = new HashSet<>();

        // 1. Custom Recipes (Priority)
        for (SkyBlockRecipe recipe : customRecipes) {
            if (canCraft(contents, recipe)) {
                ItemStack res = recipe.getResult().clone();
                // Apply Rarity
                res = plugin.getRarityManager().processItem(res);
                
                String key = RecipeMatcher.getIdentifier(res);
                if (added.add(key)) {
                    craftable.add(res);
                }
            }
        }

        // 2. Vanilla Recipes (Only if not already added by custom)
        Iterator<Recipe> it = Bukkit.recipeIterator();
        int count = 0;
        while (it.hasNext() && count < 20) {
            Recipe r = it.next();
            ItemStack res = r.getResult().clone();
            
            // Check if this material was already added via MMO/Custom
            // We want to avoid showing a vanilla item if an MMO item of the same material was added
            // OR if the same vanilla item was already added
            String vanillaId = "VANILLA:" + res.getType().name();
            
            boolean alreadyAdded = added.contains(vanillaId);
            if (!alreadyAdded) {
                // If it's a tool/armor, check if any MMO item of the same material was added
                if (res.getType().name().contains("_") || res.getType().getMaxStackSize() == 1) {
                    for (String addedKey : added) {
                        if (addedKey.startsWith("MMO:")) {
                            // This is a bit heuristic but usually works: if MMO item base material matches
                            // we might want to hide vanilla. But let's be safer:
                            // If the MMO item we added resulted in the same display name, it's definitely a duplicate.
                            // However, we don't have the MMO items here.
                        }
                    }
                }
            }

            if (!alreadyAdded && canCraftVanilla(contents, r)) {
                // Apply Rarity to vanilla results too
                res = plugin.getRarityManager().processItem(res);
                String key = RecipeMatcher.getIdentifier(res);
                if (added.add(key)) {
                    craftable.add(res);
                    count++;
                }
            }
        }
        
        return craftable;
    }

    private boolean canCraftVanilla(ItemStack[] contents, Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            return hasAllIngredients(contents, shaped.getIngredientMap().values().toArray(new ItemStack[0]));
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            return hasAllIngredients(contents, shapeless.getIngredientList().toArray(new ItemStack[0]));
        }
        return false;
    }

    private boolean canCraft(ItemStack[] inventory, SkyBlockRecipe recipe) {
        java.util.Map<String, Integer> available = new java.util.HashMap<>();
        for (ItemStack is : inventory) {
            if (is == null || is.getType() == Material.AIR) continue;
            String id = RecipeMatcher.getIdentifier(is);
            available.put(id, available.getOrDefault(id, 0) + is.getAmount());
        }

        if (recipe instanceof FlexibleSkyBlockRecipe flexible) {
            for (java.util.Map.Entry<String, Integer> req : flexible.getIngredientsMap().entrySet()) {
                if (available.getOrDefault(req.getKey(), 0) < req.getValue()) return false;
            }
            return true;
        } else if (recipe instanceof ShapedSkyBlockRecipe shaped) {
            return hasAllIngredients(inventory, shaped.getIngredients());
        } else if (recipe instanceof ShapelessSkyBlockRecipe shapeless) {
            return hasAllIngredients(inventory, shapeless.getIngredients().toArray(new ItemStack[0]));
        }
        return false;
    }

    private boolean hasAllIngredients(ItemStack[] inventory, ItemStack[] ingredients) {
        java.util.Map<String, Integer> available = new java.util.HashMap<>();
        for (ItemStack is : inventory) {
            if (is == null || is.getType() == Material.AIR) continue;
            String id = RecipeMatcher.getIdentifier(is);
            available.put(id, available.getOrDefault(id, 0) + is.getAmount());
        }

        java.util.Map<String, Integer> reqs = new java.util.HashMap<>();
        for (ItemStack is : ingredients) {
            if (is == null || is.getType() == Material.AIR) continue;
            String id = RecipeMatcher.getIdentifier(is);
            reqs.put(id, reqs.getOrDefault(id, 0) + is.getAmount());
        }

        for (java.util.Map.Entry<String, Integer> entry : reqs.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
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

    public void consumeFromInventory(org.bukkit.entity.Player player, SkyBlockRecipe recipe, ItemStack[] extraItems) {
        ItemStack[] invContents = player.getInventory().getStorageContents();
        java.util.Map<String, Integer> reqs = new java.util.HashMap<>();
        
        if (recipe instanceof FlexibleSkyBlockRecipe flexible) {
            reqs.putAll(flexible.getIngredientsMap());
        } else {
            ItemStack[] ings = (recipe instanceof ShapedSkyBlockRecipe s) ? s.getIngredients() : 
                               ((ShapelessSkyBlockRecipe)recipe).getIngredients().toArray(new ItemStack[0]);
            for (ItemStack is : ings) {
                if (is != null && is.getType() != Material.AIR) {
                    String id = RecipeMatcher.getIdentifier(is);
                    reqs.put(id, reqs.getOrDefault(id, 0) + is.getAmount());
                }
            }
        }

        consumeReqs(player, reqs, invContents, extraItems);
    }

    public void consumeVanillaFromInventory(org.bukkit.entity.Player player, org.bukkit.inventory.Recipe recipe, ItemStack[] extraItems) {
        ItemStack[] invContents = player.getInventory().getStorageContents();
        java.util.Map<String, Integer> reqs = new java.util.HashMap<>();
        
        ItemStack[] ingredients = null;
        if (recipe instanceof org.bukkit.inventory.ShapedRecipe s) ingredients = s.getIngredientMap().values().toArray(new ItemStack[0]);
        else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe s) ingredients = s.getIngredientList().toArray(new ItemStack[0]);
        
        if (ingredients != null) {
            for (ItemStack is : ingredients) {
                if (is != null && is.getType() != Material.AIR) {
                    String id = RecipeMatcher.getIdentifier(is);
                    reqs.put(id, reqs.getOrDefault(id, 0) + 1); // Vanilla ingredients are always 1
                }
            }
        }

        consumeReqs(player, reqs, invContents, extraItems);
    }

    private void consumeReqs(org.bukkit.entity.Player player, java.util.Map<String, Integer> reqs, ItemStack[] invContents, ItemStack[] extraItems) {
        for (java.util.Map.Entry<String, Integer> entry : reqs.entrySet()) {
            int remaining = entry.getValue();
            
            // 1. Extra Items (Grid)
            if (extraItems != null) {
                for (ItemStack invItem : extraItems) {
                    if (invItem != null && RecipeMatcher.getIdentifier(invItem).equals(entry.getKey())) {
                        int take = Math.min(invItem.getAmount(), remaining);
                        invItem.setAmount(invItem.getAmount() - take);
                        remaining -= take;
                        if (remaining <= 0) break;
                    }
                }
            }
            
            // 2. Inventory
            if (remaining > 0) {
                for (ItemStack invItem : invContents) {
                    if (invItem != null && RecipeMatcher.getIdentifier(invItem).equals(entry.getKey())) {
                        int take = Math.min(invItem.getAmount(), remaining);
                        invItem.setAmount(invItem.getAmount() - take);
                        remaining -= take;
                        if (remaining <= 0) break;
                    }
                }
            }
        }
        player.getInventory().setStorageContents(invContents);
    }

    public void loadRecipes() {
        customRecipes.clear();
        List<String> flexibleItems = plugin.getConfig().getStringList("flexible-items");
        Set<String> flexSet = new HashSet<>();
        for (String s : flexibleItems) flexSet.add(s.toUpperCase());

        plugin.getLogger().info("CraftingManager: Unified recipe scan starting...");
        Iterator<Recipe> it = Bukkit.recipeIterator();
        int count = 0;
        
        while (it.hasNext()) {
            try {
                Recipe recipe = it.next();
                ItemStack result = recipe.getResult();
                if (result == null || result.getType() == Material.AIR) continue;
                
                NBTItem nbt = NBTItem.get(result);
                String mmoType = RecipeMatcher.getMMOType(nbt);
                String mmoId = RecipeMatcher.getMMOId(nbt);
                
                boolean isMMO = mmoType != null;
                String identifier = isMMO ? "MMOITEMS:" + mmoType + ":" + mmoId : result.getType().name();
                identifier = identifier.toUpperCase();

                if (isMMO || flexSet.contains(identifier)) {
                    ItemStack fullResult = result;
                    if (isMMO) {
                        net.Indyuce.mmoitems.api.Type type = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(mmoType);
                        if (type != null) fullResult = net.Indyuce.mmoitems.MMOItems.plugin.getItem(type, mmoId);
                    }

                    if (flexSet.contains(identifier)) {
                        Map<String, Integer> ingredients = new HashMap<>();
                        if (recipe instanceof ShapedRecipe shaped) {
                            for (ItemStack ing : shaped.getIngredientMap().values()) {
                                if (ing != null && ing.getType() != Material.AIR) {
                                    String id = RecipeMatcher.getIdentifier(ing);
                                    ingredients.put(id, ingredients.getOrDefault(id, 0) + ing.getAmount());
                                }
                            }
                        } else if (recipe instanceof ShapelessRecipe shapeless) {
                            for (ItemStack ing : shapeless.getIngredientList()) {
                                if (ing != null && ing.getType() != Material.AIR) {
                                    String id = RecipeMatcher.getIdentifier(ing);
                                    ingredients.put(id, ingredients.getOrDefault(id, 0) + ing.getAmount());
                                }
                            }
                        }
                        if (!ingredients.isEmpty()) {
                            registerRecipe(new FlexibleSkyBlockRecipe(fullResult, ingredients));
                            count++;
                        }
                    } else if (isMMO) {
                        if (recipe instanceof ShapedRecipe shaped) {
                            ItemStack[] matrix = new ItemStack[9];
                            String[] shape = shaped.getShape();
                            java.util.Map<Character, ItemStack> ingMap = shaped.getIngredientMap();
                            for (int r = 0; r < shape.length; r++) {
                                for (int c = 0; c < shape[r].length(); c++) {
                                    matrix[r * 3 + c] = ingMap.get(shape[r].charAt(c));
                                }
                            }
                            registerRecipe(new ShapedSkyBlockRecipe(fullResult, matrix));
                            count++;
                        } else if (recipe instanceof ShapelessRecipe shapeless) {
                            registerRecipe(new ShapelessSkyBlockRecipe(fullResult, shapeless.getIngredientList()));
                            count++;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        plugin.getLogger().info("CraftingManager: Loaded " + count + " recipes from registry.");
    }

    private ItemStack parseItemString(String str) {
        if (str == null) return null;
        if (str.toUpperCase().startsWith("MMOITEMS:")) {
            String[] parts = str.split(":");
            if (parts.length >= 3) {
                String typeId = parts[1];
                String id = parts[2];
                net.Indyuce.mmoitems.api.Type type = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(typeId);
                if (type != null) return net.Indyuce.mmoitems.MMOItems.plugin.getItem(type, id);
            }
        } else {
            Material mat = Material.matchMaterial(str);
            if (mat != null) return new ItemStack(mat);
        }
        return null;
    }

    private void loadMMOItemsRecipes() {
        plugin.getLogger().info("CraftingManager: Deep-scanning Bukkit registry for MMOItems recipes...");
        Iterator<Recipe> it = Bukkit.recipeIterator();
        int count = 0;
        
        while (it.hasNext()) {
            try {
                Recipe recipe = it.next();
                ItemStack result = recipe.getResult();
                if (result == null || result.getType() == Material.AIR) continue;
                
                NBTItem nbt = NBTItem.get(result);
                    // Re-fetch the full MMOItem to get all stats and lore
                    ItemStack fullResult = null;
                    String typeId = nbt.getString("MMOITEMS_ITEM_TYPE");
                    String id = nbt.getString("MMOITEMS_ITEM_ID");
                    net.Indyuce.mmoitems.api.Type type = net.Indyuce.mmoitems.MMOItems.plugin.getTypes().get(typeId);
                    if (type != null) {
                        fullResult = net.Indyuce.mmoitems.MMOItems.plugin.getItem(type, id);
                    }
                    
                    if (fullResult == null) fullResult = result;

                    if (recipe instanceof ShapedRecipe shaped) {
                        ItemStack[] matrix = new ItemStack[9];
                        String[] shape = shaped.getShape();
                        java.util.Map<Character, ItemStack> ingMap = shaped.getIngredientMap();
                        
                        for (int r = 0; r < shape.length; r++) {
                            for (int c = 0; c < shape[r].length(); c++) {
                                char symbol = shape[r].charAt(c);
                                matrix[r * 3 + c] = ingMap.get(symbol);
                            }
                        }
                        registerRecipe(new ShapedSkyBlockRecipe(fullResult, matrix));
                        count++;
                    } else if (recipe instanceof ShapelessRecipe shapeless) {
                        registerRecipe(new ShapelessSkyBlockRecipe(fullResult, shapeless.getIngredientList()));
                        count++;
                    }
            } catch (Exception ignored) {}
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
