package dev.agam.skyblockitems.crafting.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.crafting.RecipeMatcher;
import dev.agam.skyblockitems.rarity.Rarity;
import dev.agam.skyblockitems.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class CraftingGUI implements InventoryHolder {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int[] GRID_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int RESULT_SLOT = 23;
    private static final int[] QUICK_CRAFT_SLOTS = {16, 25, 34};
    private static final int CLOSE_SLOT = 49;

    public CraftingGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        String title = plugin.getConfigManager().getMessage("crafting.gui-title");
        this.inventory = Bukkit.createInventory(this, 54, title);
        
        if (player != null) {
            initializeItems();
        }
    }

    private void initializeItems() {
        // Filler
        Material fillerMat = Material.valueOf(plugin.getConfig().getString("crafting.gui.filler", "BLACK_STAINED_GLASS_PANE"));
        ItemStack filler = new ItemStack(fillerMat);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (isGridSlot(i) || i == RESULT_SLOT || isQuickCraftSlot(i)) {
                inventory.setItem(i, new ItemStack(Material.AIR));
            } else {
                inventory.setItem(i, filler);
            }
        }

        ItemStack backToProfile = new ItemStack(Material.NETHER_STAR);
        ItemMeta backMeta = backToProfile.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ColorUtils.colorize("&aBack to Profile"));
            backToProfile.setItemMeta(backMeta);
        }
        inventory.setItem(CLOSE_SLOT, backToProfile);

        updateResult();
        updateQuickCraft();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private boolean isGridSlot(int slot) {
        for (int s : GRID_SLOTS) if (s == slot) return true;
        return false;
    }

    private boolean isQuickCraftSlot(int slot) {
        for (int s : QUICK_CRAFT_SLOTS) if (s == slot) return true;
        return false;
    }

    private ItemStack[] getGridContents() {
        ItemStack[] contents = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            contents[i] = inventory.getItem(GRID_SLOTS[i]);
        }
        return contents;
    }

    private void setGridContents(ItemStack[] contents) {
        for (int i = 0; i < 9; i++) {
            inventory.setItem(GRID_SLOTS[i], contents[i]);
        }
    }

    private void updateResult() {
        ItemStack[] matrix = getGridContents();
        Optional<ItemStack> result = plugin.getCraftingManager().findResult(matrix);
        
        if (result.isPresent()) {
            ItemStack res = result.get().clone();
            // Apply Rarity
            res = plugin.getRarityManager().processItem(res);
            
            // Apply Lore for display
            Rarity rarity = plugin.getRarityManager().getRarityForItem(res);
            if (rarity != null) {
                res = plugin.getRarityManager().updateRarityLore(res, rarity);
            }
            
            inventory.setItem(RESULT_SLOT, res);
        } else {
            ItemStack noRecipe = new ItemStack(Material.BARRIER);
            ItemMeta meta = noRecipe.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getConfigManager().getMessage("crafting.no-recipe"));
                noRecipe.setItemMeta(meta);
            }
            inventory.setItem(RESULT_SLOT, noRecipe);
        }
    }

    private void updateQuickCraft() {
        boolean hasPerm = player.hasPermission("skyblock.quickcraft") || player.isOp();
        ItemStack[] grid = getGridContents();
        
        if (!hasPerm) {
            for (int slot : QUICK_CRAFT_SLOTS) {
                ItemStack noPerm = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta = noPerm.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(plugin.getConfigManager().getMessage("crafting.no-permission-quick-craft"));
                    List<String> lore = new ArrayList<>();
                    for (String line : plugin.getConfigManager().getMessageList("crafting.quick-craft-no-perm-lore")) {
                        lore.add(ColorUtils.colorize(line));
                    }
                    meta.setLore(lore);
                    noPerm.setItemMeta(meta);
                }
                inventory.setItem(slot, noPerm);
            }
            return;
        }

        List<ItemStack> craftable = plugin.getCraftingManager().getCraftableResults(player, grid);
        for (int i = 0; i < QUICK_CRAFT_SLOTS.length; i++) {
            if (i < craftable.size()) {
                ItemStack item = craftable.get(i).clone();
                
                // Add Footer Lore FIRST
                ItemMeta qMeta = item.getItemMeta();
                if (qMeta != null) {
                    List<String> lore = qMeta.hasLore() ? qMeta.getLore() : new ArrayList<>();
                    if (!lore.isEmpty() && !lore.get(lore.size()-1).isEmpty()) lore.add("");
                    lore.add(ColorUtils.colorize("&eClick to Quick Craft!"));
                    qMeta.setLore(lore);
                    item.setItemMeta(qMeta);
                }

                // Apply Rarity Lore
                Rarity rarity = plugin.getRarityManager().getRarityForItem(item);
                if (rarity != null) {
                    item = plugin.getRarityManager().updateRarityLore(item, rarity);
                }

                inventory.setItem(QUICK_CRAFT_SLOTS[i], item);
            } else {
                inventory.setItem(QUICK_CRAFT_SLOTS[i], new ItemStack(Material.AIR));
            }
        }
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingGUI)) return;
        CraftingGUI gui = (CraftingGUI) event.getInventory().getHolder();
        if (!event.getWhoClicked().equals(gui.player)) return;

        int slot = event.getRawSlot();
        
        // Handle clicking inside the GUI
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(inventory)) {
            if (slot == CLOSE_SLOT) {
                event.setCancelled(true);
                player.closeInventory();
                player.performCommand("profile");
            } else if (slot == RESULT_SLOT) {
                event.setCancelled(true);
                handleCraft(event.isShiftClick());
            } else if (isQuickCraftSlot(slot)) {
                event.setCancelled(true);
                handleQuickCraft(slot, event.isShiftClick());
            } else if (!isGridSlot(slot)) {
                event.setCancelled(true);
            } else {
                // Check blacklist for cursor item
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR && plugin.getConfigManager().isBlacklisted(cursor)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("general.blacklisted-item"));
                    return;
                }
                
                // Check blacklist for hotbar swap
                if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                    ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                    if (hotbarItem != null && hotbarItem.getType() != Material.AIR && plugin.getConfigManager().isBlacklisted(hotbarItem)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getConfigManager().getMessage("general.blacklisted-item"));
                        return;
                    }
                }

                // Update result after any change to grid
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    gui.updateResult();
                    gui.updateQuickCraft();
                }, 1L);
            }
        } else {
            // Player inventory click
            if (event.isShiftClick()) {
                ItemStack item = event.getCurrentItem();
                if (item == null || item.getType() == Material.AIR) return;
                
                if (plugin.getConfigManager().isBlacklisted(item)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("general.blacklisted-item"));
                    return;
                }

                event.setCancelled(true);
                for (int s : GRID_SLOTS) {
                    ItemStack slotItem = inventory.getItem(s);
                    if (slotItem == null || slotItem.getType() == Material.AIR) {
                        inventory.setItem(s, item.clone());
                        event.setCurrentItem(null);
                        break;
                    }
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    gui.updateResult();
                    gui.updateQuickCraft();
                }, 1L);
            }
        }
    }

    private void handleCraft(boolean shift) {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType() == Material.AIR || result.getType() == Material.BARRIER) return;

        ItemStack[] matrix = getGridContents();
        Optional<dev.agam.skyblockitems.crafting.SkyBlockRecipe> recipe = plugin.getCraftingManager().findMatchingRecipe(matrix);

        int crafted = 0;
        int maxCrafts = shift ? 64 : 1; // Limit to 64 for safety/stack size
        
        while (crafted < maxCrafts) {
            // Check if we still have a recipe match
            Optional<ItemStack> currentResult = plugin.getCraftingManager().findResult(matrix);
            if (!currentResult.isPresent()) break;
            
            ItemStack toAdd = currentResult.get().clone();
            // Apply Rarity
            toAdd = plugin.getRarityManager().processItem(toAdd);
            
            if (player.getInventory().addItem(toAdd).isEmpty()) {
                if (recipe.isPresent()) {
                    recipe.get().consume(matrix);
                } else {
                    // Vanilla fallback
                    for (int i = 0; i < 9; i++) {
                        ItemStack item = matrix[i];
                        if (item != null && item.getType() != Material.AIR) {
                            item.setAmount(item.getAmount() - 1);
                            if (item.getAmount() <= 0) matrix[i] = null;
                        }
                    }
                }
                crafted++;
                if (!shift) break;
            } else {
                if (crafted == 0) player.sendMessage(plugin.getConfigManager().getMessage("crafting.inventory-full"));
                break;
            }
        }

        if (crafted > 0) {
            for (int i = 0; i < 9; i++) inventory.setItem(GRID_SLOTS[i], matrix[i]);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            
            updateResult();
            updateQuickCraft();
        }
    }

    private void handleQuickCraft(int slot, boolean shift) {
        if (!(player.hasPermission("skyblock.quickcraft") || player.isOp())) return;
        
        int slotIndex = -1;
        for (int i = 0; i < QUICK_CRAFT_SLOTS.length; i++) if (QUICK_CRAFT_SLOTS[i] == slot) slotIndex = i;
        if (slotIndex == -1) return;

        int totalCrafted = 0;
        int maxCrafts = shift ? 64 : 1;

        while (totalCrafted < maxCrafts) {
            ItemStack[] grid = getGridContents();
            List<ItemStack> craftable = plugin.getCraftingManager().getCraftableResults(player, grid);
            
            if (slotIndex >= craftable.size()) break;
            
            ItemStack result = craftable.get(slotIndex);
            String resultId = RecipeMatcher.getIdentifier(result);
            Optional<dev.agam.skyblockitems.crafting.SkyBlockRecipe> custom = plugin.getCraftingManager().getCustomRecipes().stream()
                    .filter(r -> RecipeMatcher.getIdentifier(r.getResult()).equals(resultId)).findFirst();
            
            // Note: getCraftableResults already processed the item with rarity
            if (player.getInventory().addItem(result.clone()).isEmpty()) {
                if (custom.isPresent()) {
                    plugin.getCraftingManager().consumeFromInventory(player, custom.get(), grid);
                    setGridContents(grid);
                } else {
                    Iterator<org.bukkit.inventory.Recipe> it = Bukkit.recipeIterator();
                    boolean found = false;
                    while (it.hasNext()) {
                        org.bukkit.inventory.Recipe r = it.next();
                        if (RecipeMatcher.getIdentifier(r.getResult()).equals(resultId)) {
                            plugin.getCraftingManager().consumeVanillaFromInventory(player, r, grid);
                            setGridContents(grid);
                            found = true;
                            break;
                        }
                    }
                    if (!found) break; // Should not happen
                }
                totalCrafted++;
                if (!shift) break;
            } else {
                if (totalCrafted == 0) player.sendMessage(plugin.getConfigManager().getMessage("crafting.inventory-full"));
                break;
            }
        }

        if (totalCrafted > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            updateResult();
            updateQuickCraft();
        }
    }


    public void handleDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingGUI)) return;
        
        ItemStack dragged = event.getOldCursor();
        if (dragged != null && dragged.getType() != Material.AIR && plugin.getConfigManager().isBlacklisted(dragged)) {
            for (int slot : event.getRawSlots()) {
                if (slot < 54) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getMessage("general.blacklisted-item"));
                    return;
                }
            }
        }

        for (int slot : event.getRawSlots()) {
            if (slot < 54 && !isGridSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.updateResult();
            this.updateQuickCraft();
        }, 1L);
    }

    public void handleClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CraftingGUI)) return;
        // Return items to player
        for (int slot : GRID_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item).values().forEach(drop -> 
                    player.getWorld().dropItemNaturally(player.getLocation(), drop));
            }
        }
    }
}
