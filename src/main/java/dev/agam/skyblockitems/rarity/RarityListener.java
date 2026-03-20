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
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listens for inventory events to automatically apply rarities to items.
 * Original logic + vanilla-parity Pick-Block (middle-click) fix.
 */
public class RarityListener implements Listener {

    private final SkyBlockItems plugin;
    private final RarityManager rarityManager;
    // Debounce for creative pick-block to prevent event loops
    private final Map<UUID, Long> lastCreativeAction = new HashMap<>();

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
     * V4.2 FIX: Don't modify items during SHIFT-CLICK to avoid breaking vanilla movement.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Skip creative pick-block — handled separately
        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) return;

        org.bukkit.inventory.Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        boolean isAllowed = rarityManager.isAllowedInventory(event.getView());
        if (!isAllowed && clickedInv.getType() != InventoryType.PLAYER) return;

        // Special handling for SHIFT-CLICK
        if (event.getClick().isShiftClick()) {
            int delay = (player.getGameMode() == org.bukkit.GameMode.CREATIVE) ? 4 : 1; 
            // Schedule inventory processing for the next tick to avoid movement interference
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) rarityManager.processInventory(player);
            }, delay);
            return;
        }

        // Regular click processing
        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && !clicked.getType().isAir()) {
            // FIX: Don't re-process if it already has our rarity NBT
            if (!rarityManager.hasCustomRarity(clicked)) {
                rarityManager.processItem(clicked);
            }
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            // FIX: Don't re-process if it already has our rarity NBT
            if (!rarityManager.hasCustomRarity(cursor)) {
                rarityManager.processItem(cursor);
            }
        }
    }

    /**
     * Process items when player picks them up.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack pickedItem = event.getCursor();
        if (pickedItem == null || pickedItem.getType() == org.bukkit.Material.AIR) return;

        // V4.1 FIX: Don't skip just because it has rarity. We need to check if 
        // we should redirect to an existing item even if the picked one has rarity!
        // String baseKey = rarityManager.getBaseKey(pickedItem);
        
        // Debounce: prevent rapid re-firing within 50ms
        long now = System.currentTimeMillis();
        Long last = lastCreativeAction.get(player.getUniqueId());
        if (last != null && now - last < 50) {
            event.setCancelled(true);
            return;
        }
        lastCreativeAction.put(player.getUniqueId(), now);

        // V5.0 FIX: Only intercept if the item is RAW (no rarity NBT).
        // If it already has NBT, it's an inventory move, and we MUST NOT touch it!
        if (rarityManager.hasCustomRarity(pickedItem)) return;

        // 1. Identify what rarity this picked item *should* have
        Rarity targetRarity = rarityManager.getRarityForItem(pickedItem);
        if (targetRarity == null) return;
        String targetRarityId = targetRarity.getIdentifier();

        int handSlot = player.getInventory().getHeldItemSlot();

        // 2. Scan for existing item to avoid duplication
        int hotbarSlot = -1;
        int inventorySlot = -1;
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem == null || invItem.getType() != pickedItem.getType()) continue;
            
            Rarity currentRarity = rarityManager.getCurrentRarity(invItem);
            String currentRarityId = currentRarity != null ? currentRarity.getIdentifier() : "NONE";
            
            if (targetRarityId.equalsIgnoreCase(currentRarityId)) {
                if (i == handSlot) return; // Already there
                if (i < 9 && hotbarSlot == -1) hotbarSlot = i;
                else if (i >= 9 && inventorySlot == -1) inventorySlot = i;
            }
        }

        if (hotbarSlot != -1 || inventorySlot != -1) {
            // Case A/B: Redirect to existing item
            final int fromSlot = (hotbarSlot != -1) ? hotbarSlot : inventorySlot;
            final int slotToSwitch = (hotbarSlot != -1) ? hotbarSlot : -1;
            
            if (slotToSwitch != -1) {
                player.getInventory().setHeldItemSlot(slotToSwitch);
                event.setCursor(null);
                event.setCancelled(true);
            } else {
                ItemStack existing = player.getInventory().getItem(fromSlot);
                if (existing != null) {
                    ItemStack toMove = existing.clone();
                    player.getInventory().setItem(fromSlot, null);
                    event.setCursor(toMove);
                }
            }
            Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
            return;
        }

        // Case 4: Not found -> Add rarity to the new cursor item
        rarityManager.processItem(pickedItem);
    }

    /**
     * Merges inventory stacks that share the same base key.
     * When rarity is applied, two previously stackable items may end up in separate
     * slots due to NBT differences. This re-merges them so the inventory stays tidy.
     */
    private void consolidateSameBaseItems(Player player, String baseKey) {
        if (baseKey == null) return;
        int masterSlot = -1;
        ItemStack master = null;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (!baseKey.equalsIgnoreCase(rarityManager.getBaseKey(item))) continue;

            if (master == null) {
                master = item;
                masterSlot = i;
            } else {
                int space = master.getMaxStackSize() - master.getAmount();
                if (space > 0) {
                    int take = Math.min(space, item.getAmount());
                    master.setAmount(master.getAmount() + take);
                    player.getInventory().setItem(masterSlot, master);
                    if (take >= item.getAmount()) {
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - take);
                    }
                }
            }
        }
        player.updateInventory();
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
