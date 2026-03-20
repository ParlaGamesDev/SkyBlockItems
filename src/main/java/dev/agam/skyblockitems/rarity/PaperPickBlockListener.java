package dev.agam.skyblockitems.rarity;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Paper-only: Fixes middle-click pick block to select existing item instead of duplicating.
 * Uses reflection so the project compiles without paper-api. On Paper servers, the event
 * is registered via EventExecutor in SkyBlockItems.
 *
 * Hotbar/in-hand: Paper's setSourceSlot causes client sync duplication in creative.
 * We cancel and handle manually (switch held slot or no-op).
 */
public class PaperPickBlockListener {

    private final Plugin plugin;
    private final RarityManager rarityManager;

    public PaperPickBlockListener(Plugin plugin, RarityManager rarityManager) {
        this.plugin = plugin;
        this.rarityManager = rarityManager;
    }

    /** Called from EventExecutor when PlayerPickBlockEvent fires ( Paper servers only ). */
    public void handle(Event event) {
        try {
            int sourceSlot = (int) event.getClass().getMethod("getSourceSlot").invoke(event);
            if (sourceSlot != -1) return;

            Object block = event.getClass().getMethod("getBlock").invoke(event);
            Material blockType = (Material) block.getClass().getMethod("getType").invoke(block);
            if (blockType.isAir()) return;

            int targetSlot = (int) event.getClass().getMethod("getTargetSlot").invoke(event);
            Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
            Object inv = player.getInventory();

            // 1. Already holding block -> cancel, no-op (prevents hotbar duplication)
            ItemStack held = (ItemStack) inv.getClass().getMethod("getItem", int.class).invoke(inv, targetSlot);
            if (held != null && held.getType() == blockType) {
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
                return;
            }

            int hotbarMatch = -1;
            int mainInvMatch = -1;
            for (int i = 0; i < 36; i++) {
                if (i == targetSlot) continue;
                ItemStack invItem = (ItemStack) inv.getClass().getMethod("getItem", int.class).invoke(inv, i);
                if (invItem == null || invItem.getType() != blockType) continue;
                if (i < 9 && hotbarMatch == -1) hotbarMatch = i;
                else if (i >= 9 && mainInvMatch == -1) mainInvMatch = i;
            }

            int useSlot = hotbarMatch != -1 ? hotbarMatch : mainInvMatch;
            if (useSlot == -1) return;

            // 2. Block in HOTBAR -> cancel (Paper duplicates). Manually swap items instead.
            if (useSlot < 9) {
                event.getClass().getMethod("setCancelled", boolean.class).invoke(event, true);
                org.bukkit.inventory.PlayerInventory pi = player.getInventory();
                ItemStack inHand = pi.getItem(targetSlot);
                ItemStack inOther = pi.getItem(useSlot);
                pi.setItem(targetSlot, inOther);
                pi.setItem(useSlot, inHand);
                return;
            }

            // 3. Block in MAIN inventory -> setSourceSlot works (no duplication)
            event.getClass().getMethod("setSourceSlot", int.class).invoke(event, useSlot);
        } catch (Throwable ignored) {
        }
    }
}
