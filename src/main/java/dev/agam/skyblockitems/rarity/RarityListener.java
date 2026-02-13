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
import net.Indyuce.mmoitems.api.event.ItemBuildEvent;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
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

        // Efficiently scan player inventory and container in a single delayed task
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;

            // 1. Scan player inventory
            rarityManager.processInventory(player);

            // 2. Scan container if allowed
            if (rarityManager.isAllowedInventory(event.getView())) {
                org.bukkit.inventory.Inventory topInv = event.getView().getTopInventory();
                if (topInv != null && topInv.getType() != InventoryType.CRAFTING) {
                    processInventory(topInv);
                }
            }
        }, 2L); // 2-tick delay to be safe
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
                rarityManager.processInventory(player);
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

            // 2. Double click / Shift click handling removed for performance
            // The item will be processed when interacted with next time or on inventory
            // close/open.

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
        rarityManager.processInventory(player);
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

    /**
     * Force-injects custom rarity during MMOItems build process.
     * This ensures the item is "born" with the correct rarity if a record exists in
     * rarity.yml.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMMOItemBuild(ItemBuildEvent event) {
        if (!plugin.isMMOItemsEnabled())
            return;

        ItemStack item = event.getItemStack();
        Rarity customRarity = rarityManager.getRarityForItem(item);

        // If we found a record in rarity.yml (Sharp Logic)
        if (customRarity != null) {
            ItemStack processed = rarityManager.applyRarity(item, customRarity, true);
            event.setItemStack(processed);
        }
    }
}
