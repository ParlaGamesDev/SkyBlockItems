package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class RarityUpdateTask extends BukkitRunnable {

    private final SkyBlockItems plugin;

    public RarityUpdateTask(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateInventory(player);
        }
    }

    private void updateInventory(Player player) {
        // Main Inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                plugin.getRarityManager().updateLore(item);
            }
        }
        // Armor
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && !item.getType().isAir()) {
                plugin.getRarityManager().updateLore(item);
            }
        }
        // Offhand (if 1.9+)
        try {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && !offhand.getType().isAir()) {
                plugin.getRarityManager().updateLore(offhand);
            }
        } catch (NoSuchMethodError ignored) {
        }
    }
}
