package dev.agam.skyblockitems.rarity;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * Paper-only: creative middle-click pick block + deferred rarity. Survival is untouched (vanilla).
 */
public class PaperPickBlockListener {

    private final RarityManager rarityManager;

    public PaperPickBlockListener(RarityManager rarityManager) {
        this.rarityManager = rarityManager;
    }

    /** Called from EventExecutor when PlayerPickBlockEvent fires ( Paper servers only ). */
    public void handle(Event event) {
        try {
            Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
            if (player.getGameMode() != GameMode.CREATIVE) {
                return;
            }

            int sourceSlot = (int) event.getClass().getMethod("getSourceSlot").invoke(event);
            if (sourceSlot != -1) {
                return;
            }

            Object block = event.getClass().getMethod("getBlock").invoke(event);
            Material blockType = (Material) block.getClass().getMethod("getType").invoke(block);
            if (blockType.isAir()) {
                return;
            }

            int targetSlot = (int) event.getClass().getMethod("getTargetSlot").invoke(event);
            Object inv = player.getInventory();

            ItemStack held = (ItemStack) inv.getClass().getMethod("getItem", int.class).invoke(inv, targetSlot);
            if (held != null && held.getType() == blockType) {
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
                return;
            }

            int hotbarMatch = -1;
            int mainInvMatch = -1;
            for (int i = 0; i < 36; i++) {
                if (i == targetSlot) {
                    continue;
                }
                ItemStack invItem = (ItemStack) inv.getClass().getMethod("getItem", int.class).invoke(inv, i);
                if (invItem == null || invItem.getType() != blockType) {
                    continue;
                }
                if (i < 9 && hotbarMatch == -1) {
                    hotbarMatch = i;
                } else if (i >= 9 && mainInvMatch == -1) {
                    mainInvMatch = i;
                }
            }

            int useSlot = hotbarMatch != -1 ? hotbarMatch : mainInvMatch;
            if (useSlot == -1) {
                rarityManager.deferInventoryRarityApply(player);
                return;
            }

            if (useSlot < 9) {
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
                org.bukkit.inventory.PlayerInventory pi = player.getInventory();
                ItemStack inHand = pi.getItem(targetSlot);
                ItemStack inOther = pi.getItem(useSlot);
                pi.setItem(targetSlot, inOther);
                pi.setItem(useSlot, inHand);
                rarityManager.deferInventoryRarityApply(player);
                return;
            }

            event.getClass().getMethod("setSourceSlot", int.class).invoke(event, useSlot);
            rarityManager.deferInventoryRarityApply(player);
        } catch (Throwable ignored) {
        }
    }
}
