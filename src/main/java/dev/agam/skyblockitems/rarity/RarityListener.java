package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for inventory events to automatically apply rarities to items.
 */
public class RarityListener implements Listener {

    private final SkyBlockItems plugin;
    private final RarityManager rarityManager;

    public RarityListener(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.rarityManager = plugin.getRarityManager();
    }

    /**
     * Process all items when player joins the server.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Process entire inventory on join with a small delay
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                processPlayerInventory(player);
            }
        }, 5L);
    }

    /**
     * Process items when player opens an inventory.
     * Also scans containers (chests, barrels, etc) to ensure they are up to date.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Always scan player's own inventory
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                processInventory(player.getInventory());
            }
        }, 1L);

        // Scan the opened container ONLY if it's allowed (not a GUI)
        if (rarityManager.isAllowedInventory(event.getView())) {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    org.bukkit.inventory.Inventory topInv = event.getView().getTopInventory();
                    if (topInv != null && topInv.getType() != InventoryType.CRAFTING) {
                        processInventory(topInv);
                    }
                }
            }, 1L);
        } else {
            rarityManager.debug("Skipping container scan for " + event.getView().getTitle() + " (GUI detected)");
        }
    }

    /**
     * Process items when player closes an inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        // Process player's inventory after closing
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                processInventory(player.getInventory());
            }
        }, 1L);
    }

    /**
     * Process items when player clicks in their inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if the inventory being clicked is allowed
        org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
        boolean isAllowed = (clickedInv != null) && rarityManager.isAllowedInventory(event.getView());

        // Use a 1-tick delay to allow the item to physically move before processing
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;

            // 1. Process the clicked slot if it's allowed or part of the player's inventory
            if (clickedInv != null && (isAllowed || clickedInv.getType() == InventoryType.PLAYER)) {
                ItemStack item = event.getClickedInventory().getItem(event.getSlot());
                if (item != null && !item.getType().isAir()) {
                    ItemStack processed = rarityManager.processItem(item);
                    if (processed != item) {
                        event.getClickedInventory().setItem(event.getSlot(), processed);
                    }
                }
            }

            // 2. Optimized: If the click moved items into the player's inventory (Shift,
            // Hotbar, Collect)
            // we scan the player's inventory immediately to ensure fast updates.
            if (event.isShiftClick() ||
                    event.getClick().name().contains("HOTBAR") ||
                    event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
                processInventory(player.getInventory());
            }

            // 3. Always process cursor item (for regular clicks taking items into hand)
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                ItemStack processed = rarityManager.processItem(cursor);
                if (processed != cursor) {
                    player.setItemOnCursor(processed);
                }
            }
        }, 1L);
    }

    /**
     * Process items when player picks them up.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        ItemStack item = event.getItem().getItemStack();
        ItemStack processed = rarityManager.processItem(item);
        if (processed != item) {
            event.getItem().setItemStack(processed);
        }
    }

    /**
     * Process the item when player switches held slot.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && !item.getType().isAir()) {
            ItemStack processed = rarityManager.processItem(item);
            if (processed != item) {
                player.getInventory().setItem(event.getNewSlot(), processed);
            }
        }
    }

    /**
     * Helper method to process all items in a player's inventory.
     */
    private void processPlayerInventory(Player player) {
        processInventory(player.getInventory());
    }

    /**
     * Helper method to process all items in any inventory.
     */
    private void processInventory(org.bukkit.inventory.Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                ItemStack processed = rarityManager.processItem(item);
                if (processed != item) {
                    inventory.setItem(i, processed);
                }
            }
        }
    }
}
