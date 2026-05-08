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
        String title = ColorUtils.colorize(plugin.getConfig().getString("crafting.title", "&8Crafting Table"));
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
                meta.setDisplayName(ColorUtils.colorize(plugin.getConfig().getString("crafting.messages.no-recipe", "&cNo Recipe Found")));
                noRecipe.setItemMeta(meta);
            }
            inventory.setItem(RESULT_SLOT, noRecipe);
        }
    }

    private void updateQuickCraft() {
        boolean hasPerm = player.hasPermission("skyblock.quickcraft") || player.isOp();
        ItemStack[] grid = getGridContents();
        
        for (int slot : QUICK_CRAFT_SLOTS) {
            if (!hasPerm) {
                ItemStack noPerm = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta = noPerm.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ColorUtils.colorize(plugin.getConfig().getString("crafting.messages.no-permission-quick-craft", "&cNo Permission")));
                    List<String> lore = new ArrayList<>();
                    for (String line : plugin.getConfig().getStringList("crafting.gui.quick-craft-no-perm-lore")) {
                        lore.add(ColorUtils.colorize(line));
                    }
                    meta.setLore(lore);
                    noPerm.setItemMeta(meta);
                }
                inventory.setItem(slot, noPerm);
            } else {
                List<ItemStack> craftable = plugin.getCraftingManager().getCraftableResults(player, grid);
                int index = 0;
                for (int i = 0; i < QUICK_CRAFT_SLOTS.length; i++) {
                    if (index < craftable.size()) {
                        ItemStack item = craftable.get(index).clone();
                        
                        // Add Footer Lore FIRST
                        ItemMeta qMeta = item.getItemMeta();
                        if (qMeta != null) {
                            List<String> lore = qMeta.hasLore() ? qMeta.getLore() : new ArrayList<>();
                            if (!lore.isEmpty() && !lore.get(lore.size()-1).isEmpty()) lore.add("");
                            lore.add(ColorUtils.colorize("&eClick to Quick Craft!"));
                            qMeta.setLore(lore);
                            item.setItemMeta(qMeta);
                        }

                        // Apply Rarity Lore (it will now handle the footer correctly)
                        Rarity rarity = plugin.getRarityManager().getRarityForItem(item);
                        if (rarity != null) {
                            item = plugin.getRarityManager().updateRarityLore(item, rarity);
                        }

                        inventory.setItem(QUICK_CRAFT_SLOTS[i], item);
                        index++;
                    } else {
                        inventory.setItem(QUICK_CRAFT_SLOTS[i], new ItemStack(Material.AIR));
                    }
                }
                break;
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
                handleCraft();
            } else if (isQuickCraftSlot(slot)) {
                event.setCancelled(true);
                handleQuickCraft(slot);
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

    private void handleCraft() {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (result == null || result.getType() == Material.AIR || result.getType() == Material.BARRIER) return;

        ItemStack[] matrix = getGridContents();
        Optional<dev.agam.skyblockitems.crafting.SkyBlockRecipe> recipe = plugin.getCraftingManager().findMatchingRecipe(matrix);

        if (player.getInventory().addItem(result.clone()).isEmpty()) {
            if (recipe.isPresent()) {
                recipe.get().consume(matrix);
                for (int i = 0; i < 9; i++) inventory.setItem(GRID_SLOTS[i], matrix[i]);
            } else {
                for (int slot : GRID_SLOTS) {
                    ItemStack item = inventory.getItem(slot);
                    if (item != null) {
                        item.setAmount(item.getAmount() - 1);
                        if (item.getAmount() <= 0) inventory.setItem(slot, null);
                    }
                }
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
            
            String craftMsg = plugin.getConfig().getString("crafting.messages.crafted-item", "&aYou crafted &f%item%&a!");
            String itemName = result.hasItemMeta() && result.getItemMeta().hasDisplayName() ? 
                             result.getItemMeta().getDisplayName() : result.getType().name();
            player.sendMessage(ColorUtils.colorize(craftMsg.replace("%item%", itemName)));
            
            updateResult();
        } else {
            player.sendMessage(ColorUtils.colorize(plugin.getConfig().getString("crafting.messages.inventory-full", "&cYour inventory is full!")));
        }
    }

    private void handleQuickCraft(int slot) {
        if (!(player.hasPermission("skyblock.quickcraft") || player.isOp())) return;
        
        ItemStack[] grid = getGridContents();
        List<ItemStack> craftable = plugin.getCraftingManager().getCraftableResults(player, grid);
        int index = -1;
        for (int i = 0; i < QUICK_CRAFT_SLOTS.length; i++) if (QUICK_CRAFT_SLOTS[i] == slot) index = i;
        
        if (index >= 0 && index < craftable.size()) {
            ItemStack result = craftable.get(index);
            Optional<dev.agam.skyblockitems.crafting.SkyBlockRecipe> custom = plugin.getCraftingManager().getCustomRecipes().stream()
                    .filter(r -> r.getResult().isSimilar(result)).findFirst();
            
            if (player.getInventory().addItem(result.clone()).isEmpty()) {
                if (custom.isPresent()) {
                    plugin.getCraftingManager().consumeFromInventory(player, custom.get(), grid);
                    setGridContents(grid);
                } else {
                    Iterator<org.bukkit.inventory.Recipe> it = Bukkit.recipeIterator();
                    while (it.hasNext()) {
                        org.bukkit.inventory.Recipe r = it.next();
                        if (r.getResult().isSimilar(result)) {
                            consumeVanillaCombined(r, grid);
                            setGridContents(grid);
                            break;
                        }
                    }
                }
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 2.0f);
                updateResult();
                updateQuickCraft();
            }
        }
    }

    private void consumeVanillaCombined(org.bukkit.inventory.Recipe recipe, ItemStack[] grid) {
        ItemStack[] ingredients = null;
        if (recipe instanceof org.bukkit.inventory.ShapedRecipe s) ingredients = s.getIngredientMap().values().toArray(new ItemStack[0]);
        else if (recipe instanceof org.bukkit.inventory.ShapelessRecipe s) ingredients = s.getIngredientList().toArray(new ItemStack[0]);
        
        if (ingredients != null) {
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (ItemStack req : ingredients) {
                if (req == null) continue;
                int remaining = req.getAmount();
                
                for (ItemStack inv : grid) {
                    if (inv != null && inv.getType() == req.getType()) {
                        int take = Math.min(inv.getAmount(), remaining);
                        inv.setAmount(inv.getAmount() - take);
                        remaining -= take;
                        if (remaining <= 0) break;
                    }
                }
                if (remaining > 0) {
                    for (ItemStack inv : contents) {
                        if (inv != null && inv.getType() == req.getType()) {
                            int take = Math.min(inv.getAmount(), remaining);
                            inv.setAmount(inv.getAmount() - take);
                            remaining -= take;
                            if (remaining <= 0) break;
                        }
                    }
                }
            }
            player.getInventory().setStorageContents(contents);
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
