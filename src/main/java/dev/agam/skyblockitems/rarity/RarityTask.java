package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Backup task that periodically scans player inventories for items without
 * rarity.
 */
public class RarityTask extends BukkitRunnable {

    private final SkyBlockItems plugin;
    private final RarityManager rarityManager;

    public RarityTask(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.rarityManager = plugin.getRarityManager();
    }

    @Override
    public void run() {
        /* Disabled: processInventory while player is online causes inventory freeze/flicker.
         * Rarity is applied on: join (delayed), pickup, item spawn. */
    }

    /**
     * Processes all items in a player's inventory.
     */
    private void processPlayerInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                ItemStack processed = rarityManager.processItem(item);
                if (processed != item) {
                    player.getInventory().setItem(i, processed);
                }
            }
        }
    }
}
