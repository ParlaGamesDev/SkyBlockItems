package dev.agam.skyblockitems.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Global listener to prevent items from losing durability.
 */
public class DurabilityListener implements Listener {

    private final SkyBlockItems plugin;

    public DurabilityListener(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        // Prevent all items from losing durability globally
        event.setCancelled(true);
    }
}
