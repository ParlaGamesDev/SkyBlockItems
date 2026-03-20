package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listens for inventory events to automatically apply rarities to items.
 * Original logic + vanilla-parity Pick-Block (middle-click) fix.
 */
public class RarityListener implements Listener {

    private final SkyBlockItems plugin;
    private final RarityManager rarityManager;

    public RarityListener(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.rarityManager = plugin.getRarityManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) rarityManager.processInventory(player);
        }, 40L);
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

}
