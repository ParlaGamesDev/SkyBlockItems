package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
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
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            GameMode gm = player.getGameMode();
            // Creative client owns inventory sync; scanning every tick causes flicker, dupes, vanishing items.
            if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) {
                continue;
            }
            if (rarityManager.shouldDeferPeriodicInventoryScan(player)) {
                continue;
            }
            rarityManager.processInventory(player, false);
        }
    }
}
