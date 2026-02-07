package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

public class RarityListener implements Listener {

    private final SkyBlockItems plugin;
    private final RarityManager manager;

    public RarityListener(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.manager = plugin.getRarityManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        ItemStack item = event.getItem().getItemStack();
        if (isValidItem(item)) {
            manager.updateLore(item);
            event.getItem().setItemStack(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        // Creative mode handling is problematic with InventoryClickEvent (client-side
        // duplication).
        // relying on RarityUpdateTask for creative players is safer.
        if (((Player) event.getWhoClicked()).getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        // ... rest of logic
        if (isValidItem(current)) {
            manager.updateLore(current);
            event.setCurrentItem(current);
        }

        ItemStack cursor = event.getCursor();
        if (isValidItem(cursor)) {
            manager.updateLore(cursor);
            event.getView().setCursor(cursor);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;

        // Scan the opened inventory immediately
        for (ItemStack item : event.getInventory().getContents()) {
            if (isValidItem(item)) {
                manager.updateLore(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (ItemStack item : player.getInventory().getContents()) {
            if (isValidItem(item)) {
                manager.updateLore(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftPrepare(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (isValidItem(result)) {
            manager.updateLore(result);
            event.getInventory().setResult(result);
        }
    }

    private boolean isValidItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }
}
