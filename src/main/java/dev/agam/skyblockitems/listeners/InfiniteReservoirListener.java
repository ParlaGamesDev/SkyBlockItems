package dev.agam.skyblockitems.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener for Infinite Reservoir ability - prevents water bucket from
 * emptying.
 */
public class InfiniteReservoirListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        ItemStack bucket = event.getItemStack();

        if (bucket == null || bucket.getType() != Material.WATER_BUCKET) {
            return;
        }

        NBTItem nbtItem = NBTItem.get(bucket);
        if (nbtItem == null || !nbtItem.hasTag("SKYBLOCK_INFINITE_RESERVOIR")) {
            return;
        }

        // Check if the tag value is true (boolean support)
        Object tagValue = nbtItem.getBoolean("SKYBLOCK_INFINITE_RESERVOIR");
        if (tagValue == null || !((Boolean) tagValue)) {
            // Try string check as fallback
            String stringValue = nbtItem.getString("SKYBLOCK_INFINITE_RESERVOIR");
            if (stringValue == null || stringValue.isEmpty() || stringValue.equalsIgnoreCase("false")) {
                return;
            }
        }

        // Store reference to the original bucket NBT data
        final ItemStack originalBucket = bucket.clone();

        // Schedule a task to restore the water bucket after the event
        new BukkitRunnable() {
            @Override
            public void run() {
                // Restore the water bucket in the player's hand
                if (player.isOnline()) {
                    ItemStack mainHand = player.getInventory().getItemInMainHand();
                    ItemStack offHand = player.getInventory().getItemInOffHand();

                    // Check which hand had the bucket and restore it
                    if (mainHand.getType() == Material.BUCKET) {
                        player.getInventory().setItemInMainHand(originalBucket);
                    } else if (offHand.getType() == Material.BUCKET) {
                        player.getInventory().setItemInOffHand(originalBucket);
                    }
                }
            }
        }.runTaskLater(SkyBlockItems.getInstance(), 1L);
    }
}
