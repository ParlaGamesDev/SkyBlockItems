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
import net.Indyuce.mmoitems.api.event.ItemBuildEvent;
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

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null && !clicked.getType().isAir()) {
            ItemStack processed = rarityManager.processItem(clicked);
            if (processed != clicked) event.setCurrentItem(processed);
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            ItemStack processed = rarityManager.processItem(cursor);
            if (processed != cursor) event.setCursor(processed);
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

        String baseKey = rarityManager.getBaseKey(pickedItem);
        if (baseKey == null) return;

        int handSlot = player.getInventory().getHeldItemSlot();
        int eventSlot = event.getSlot();

        // 1. Check if the player is already holding a matching item in their hand
        ItemStack heldItem = player.getInventory().getItem(handSlot);
        if (baseKey.equalsIgnoreCase(rarityManager.getBaseKey(heldItem))) {
            event.setCancelled(true);
            player.updateInventory(); // Force sync to stop client cursor creation
            return;
        }

        // Scan inventory for existing match (hotbar first, then main inventory)
        int hotbarSlot = -1;
        int inventorySlot = -1;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (i == handSlot) continue;
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem == null || invItem.getType().isAir()) continue;
            if (baseKey.equalsIgnoreCase(rarityManager.getBaseKey(invItem))) {
                if (i < 9 && hotbarSlot == -1) hotbarSlot = i;
                else if (i >= 9 && inventorySlot == -1) inventorySlot = i;
            }
        }

        if (hotbarSlot != -1) {
            // 2. Found in another hotbar slot -> switch to it
            event.setCancelled(true);
            player.getInventory().setHeldItemSlot(hotbarSlot);
            player.updateInventory();
            return;
        }

        if (inventorySlot != -1) {
            // 3. Found in main inventory -> swap it with the hotbar slot
            event.setCancelled(true);
            final int fromSlot = inventorySlot;
            final ItemStack existing = player.getInventory().getItem(fromSlot).clone();
            
            // Perform swap in next tick to avoid inventory state conflicts
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.getInventory().setItem(fromSlot, player.getInventory().getItem(handSlot));
                player.getInventory().setItem(handSlot, existing);
                player.updateInventory();
            }, 1L);
            return;
        }

        // 4. Not in inventory -> Provide a new item with rarity applied
        // Instead of canceling and giving manually, we modify the cursor item 
        // so Minecraft places the rarity-version directly.
        ItemStack newItemWithRarity = rarityManager.processItem(pickedItem.clone());
        if (newItemWithRarity != null && !newItemWithRarity.equals(pickedItem)) {
            event.setCursor(newItemWithRarity);
            // After 1 tick, check for any duplicates Minecraft might have slipped in 
            // due to creative mode's weird sync
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) consolidateSameBaseItems(player, baseKey);
            }, 5L);
        }
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

    /**
     * Force-injects custom rarity during MMOItems build process.
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMMOItemBuild(ItemBuildEvent event) {
        if (!plugin.isMMOItemsEnabled()) return;

        ItemStack item = event.getItemStack();
        Rarity targetRarity = rarityManager.getRarityForItem(item);

        if (targetRarity != null) {
            boolean isTrulyCustom = rarityManager.hasCustomRarity(item);
            ItemStack processed = rarityManager.applyRarity(item, targetRarity, isTrulyCustom);
            event.setItemStack(processed);
        }
    }
}
