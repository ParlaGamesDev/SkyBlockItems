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
 * Uses a 1-tick delay to ensure the bucket is restored even if the event
 * logic normally replaces it with an empty bucket.
 */
public class InfiniteReservoirListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();

        // Find which hand is being used for the bucket
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        ItemStack usedBucket = null;
        boolean isMainHand = false;

        if (mainHand != null && mainHand.getType() == Material.WATER_BUCKET) {
            usedBucket = mainHand;
            isMainHand = true;
        } else if (offHand != null && offHand.getType() == Material.WATER_BUCKET) {
            usedBucket = offHand;
            isMainHand = false;
        }

        if (usedBucket == null)
            return;

        NBTItem nbtItem = NBTItem.get(usedBucket);
        if (nbtItem == null || !nbtItem.hasTag("SKYBLOCK_INFINITE_RESERVOIR")) {
            return;
        }

        // Clone the bucket
        final ItemStack bucketToRestore = usedBucket.clone();
        final boolean mainHandUsed = isMainHand;

        // Restore the bucket after a tiny delay to override server empty bucket logic
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline())
                    return;

                if (mainHandUsed) {
                    player.getInventory().setItemInMainHand(bucketToRestore);
                } else {
                    player.getInventory().setItemInOffHand(bucketToRestore);
                }
                player.updateInventory();
            }
        }.runTaskLater(SkyBlockItems.getInstance(), 1L);
    }
}
