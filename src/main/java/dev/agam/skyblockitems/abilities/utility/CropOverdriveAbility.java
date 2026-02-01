package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CropOverdriveAbility extends SkyBlockAbility implements Listener {

    private final Set<UUID> activeUsers = new HashSet<>();

    public CropOverdriveAbility() {
        super("CROP_OVERDRIVE", "חקלאות מואצת", TriggerType.RIGHT_CLICK, 60.0, 0.0, 10.0, 0.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double duration,
            double range) {
        // Save a reference to the item BEFORE it might change
        final ItemStack heldItem = player.getInventory().getItemInMainHand().clone();

        UUID uuid = player.getUniqueId();
        activeUsers.add(uuid);
        String startMsg = SkyBlockItems.getInstance().getMessagesConfig().getString("players.crop-overdrive-start",
                "&a🌾 היכולת הופעלה! למשך {duration} שניות כל יבול שתקצור יישתל מחדש אוטומטית!");
        dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player,
                startMsg.replace("{duration}", String.valueOf((int) duration)));

        Bukkit.getScheduler().runTaskLater(SkyBlockItems.getInstance(), () -> {
            activeUsers.remove(uuid);
            if (player.isOnline()) {
                // Consume item at the END of the duration
                ItemStack currentItem = player.getInventory().getItemInMainHand();
                // Check if the player still holds the same item (by comparing NBT)
                if (currentItem != null && !currentItem.getType().isAir()) {
                    io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(currentItem);
                    if (nbt.hasTag("SKYBLOCK_CROP_OVERDRIVE")) {
                        if (currentItem.getAmount() > 1) {
                            currentItem.setAmount(currentItem.getAmount() - 1);
                        } else {
                            player.getInventory().setItemInMainHand(null);
                        }
                    }
                }
                String endMsg = SkyBlockItems.getInstance().getMessagesConfig().getString("players.crop-overdrive-end",
                        "&c⌚ השפעת החקלאות המואצת נגמרה.");
                dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, endMsg);
            }
        }, (long) (duration * 20));

        return true;
    }

    @EventHandler
    public void onCropHarvest(BlockBreakEvent event) {
        if (!activeUsers.contains(event.getPlayer().getUniqueId()))
            return;

        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Ageable))
            return;

        Material seedType = getSeedForCrop(block.getType());
        if (seedType == null)
            return;

        // Replant after a short tick to ensure break finished
        Bukkit.getScheduler().runTask(SkyBlockItems.getInstance(), () -> {
            block.setType(block.getType());
            Ageable ageable = (Ageable) block.getBlockData();
            ageable.setAge(0);
            block.setBlockData(ageable);
        });
    }

    private Material getSeedForCrop(Material crop) {
        switch (crop) {
            case WHEAT:
                return Material.WHEAT_SEEDS;
            case CARROTS:
                return Material.CARROT;
            case POTATOES:
                return Material.POTATO;
            case BEETROOTS:
                return Material.BEETROOT_SEEDS;
            case NETHER_WART:
                return Material.NETHER_WART;
            default:
                return null;
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
