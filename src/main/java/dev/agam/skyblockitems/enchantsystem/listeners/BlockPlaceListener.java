package dev.agam.skyblockitems.enchantsystem.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Listener to track player-placed blocks to prevent exploits with farming
 * enchantments.
 */
public class BlockPlaceListener implements Listener {

    private final SkyBlockItems plugin;

    public BlockPlaceListener(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Mark the block as placed by a player
        event.getBlock().setMetadata("PLACED_BY_PLAYER", new FixedMetadataValue(plugin, true));
    }
}
