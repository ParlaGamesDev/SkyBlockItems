package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for inventory events to automatically apply rarities to items.
 * Original logic + vanilla-parity Pick-Block (middle-click) fix.
 */
public class RarityListener implements Listener {

    private final SkyBlockItems plugin;
    private final RarityManager rarityManager;
    /** Coalesce rapid inventory updates so we process after vanilla finishes click/drag. */
    private final Map<UUID, Integer> pendingInventoryProcessTaskIds = new ConcurrentHashMap<>();

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
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                processPlayerInventory(player);
            }
        }, 5L);
    }

    /**
     * Process items when player opens an inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            rarityManager.processInventory(player);
            if (rarityManager.isAllowedInventory(event.getView())) {
                org.bukkit.inventory.Inventory topInv = event.getView().getTopInventory();
                if (topInv != null && topInv.getType() != InventoryType.CRAFTING) {
                    processInventory(topInv);
                }
            }
        }, 2L);
    }

    /**
     * Process items when player closes an inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) rarityManager.processInventory(player);
        }, 1L);
    }

    /**
     * Process items when player clicks in their inventory.
     * Never mutate current/cursor during the click — that breaks creative drag-to-distribute
     * and stack merging. Defer a full inventory pass until vanilla finishes the action.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Middle / creative pick block is InventoryCreativeEvent, not this handler
        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) return;

        org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        boolean isAllowed = rarityManager.isAllowedInventory(event.getView());
        if (!isAllowed && clickedInv.getType() != InventoryType.PLAYER) return;

        long delay = 0L;
        scheduleDeferredInventoryProcess(player, delay);
    }

    /**
     * Creative middle-click drag-to-distribute uses drag events, not click events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlots().isEmpty()) return;
        scheduleDeferredInventoryProcess(player, 0L);
    }

    /**
     * Process items when player picks them up.
     */
    /**
     * Apply rarity before the pickup merges into the inventory so stacks match vanilla.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack item = event.getItem().getItemStack();
        ItemStack processed = rarityManager.processItem(item);
        if (processed != item) event.getItem().setItemStack(processed);
    }

    /**
     * Process items as soon as they spawn in the world.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        ItemStack processed = rarityManager.processItem(item);
        if (processed != item) event.getEntity().setItemStack(processed);
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
            if (processed != item) player.getInventory().setItem(event.getNewSlot(), processed);
        }
    }

    /**
     * Vanilla-parity Creative Pick Block (middle-click) fix.
     *
     * Problem: Minecraft doesn't know that a vanilla "Stone" block is the same as
     * "Common Stone" (with rarity NBT) already in the inventory, so it keeps adding
     * new items instead of selecting the existing one.
     *
     * Logic:
     * 1. Already holding matching item → do nothing.
     * 2. Item in another hotbar slot → switch to that slot.
     * 3. Item in main inventory → move it to hand.
     * 4. Not found anywhere → cancel vanilla, give new item with rarity, consolidate stacks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        // Never touch shift-click — user is moving items between inventories; cancelling makes them disappear
        if (event.isShiftClick()) return;

        // Pick-block redirect is handled by PaperPickBlockListener (Paper's PlayerPickBlockEvent).
        // On Spigot without Paper, we don't intercept — InventoryCreativeEvent doesn't fire reliably for pick block.
    }

    private void scheduleDeferredInventoryProcess(Player player, long delayTicks) {
        UUID uuid = player.getUniqueId();
        Integer existing = pendingInventoryProcessTaskIds.remove(uuid);
        if (existing != null) {
            Bukkit.getScheduler().cancelTask(existing);
        }
        long delay = Math.max(0L, delayTicks);
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingInventoryProcessTaskIds.remove(uuid);
            if (player.isOnline()) {
                rarityManager.processInventory(player);
            }
        }, delay).getTaskId();
        pendingInventoryProcessTaskIds.put(uuid, taskId);
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
}
