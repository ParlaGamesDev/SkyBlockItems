package dev.agam.skyblockitems.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.stats.PermissionVoucherStat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Consumes items that carry {@link PermissionVoucherStat} on right-click and grants
 * one permission via Vault. Stat value format: {@code permission,message} — first
 * comma separates the permission from the chat message (rest of the string).
 */
public class PermissionVoucherListener implements Listener {

    private static final String NO_PROVIDER = "&cמערכת ההרשאות לא זמינה. פנה למנהל השרת.";

    private final SkyBlockItems plugin;

    public PermissionVoucherListener(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getAmount() < 1) {
            return;
        }
        String raw = PermissionVoucherStat.getRawPermissionString(item);
        if (raw == null) {
            return;
        }
        Parsed parsed = Parsed.split(raw);
        if (parsed.permission.isEmpty()) {
            return;
        }

        if (!plugin.getVaultHook().isPermissionEnabled()) {
            player.sendMessage(ColorUtils.colorize(NO_PROVIDER.replace("{player}", player.getName())));
            return;
        }

        plugin.getVaultHook().addPermission(player, parsed.permission);

        int amount = item.getAmount();
        if (hand == EquipmentSlot.HAND) {
            if (amount <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(amount - 1);
                player.getInventory().setItemInMainHand(item);
            }
        } else {
            if (amount <= 1) {
                player.getInventory().setItemInOffHand(null);
            } else {
                item.setAmount(amount - 1);
                player.getInventory().setItemInOffHand(item);
            }
        }

        event.setCancelled(true);

        if (parsed.chatMessage != null && !parsed.chatMessage.isEmpty()) {
            player.sendMessage(ColorUtils.colorize(
                    parsed.chatMessage.replace("{player}", player.getName())));
        }
    }

    private static final class Parsed {
        final String permission;
        /** Text after the first comma; may be null if no comma. */
        final String chatMessage;

        Parsed(String permission, String chatMessage) {
            this.permission = permission;
            this.chatMessage = chatMessage;
        }

        static Parsed split(String raw) {
            int i = raw.indexOf(',');
            if (i < 0) {
                return new Parsed(raw.trim(), null);
            }
            String perm = raw.substring(0, i).trim();
            String rest = raw.substring(i + 1);
            return new Parsed(perm, rest);
        }
    }
}
