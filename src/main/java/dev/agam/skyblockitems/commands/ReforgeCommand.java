package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.reforge.gui.ReforgeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for /reforge.
 * Opens the reforge GUI for players or handles admin operations.
 */
public class ReforgeCommand implements CommandExecutor {

    private final SkyBlockItems plugin;

    public ReforgeCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 1. Handle Admin Subcommands
        if (args.length > 0) {
            String sub = args[0].toLowerCase();

            // sbi reforge reload
            if (sub.equals("reload")) {
                if (!sender.hasPermission("skyblock.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                plugin.getReforgeManager().reload();
                sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload-success"));
                return true;
            }

            // sbi reforge vip
            if (sub.equals("vip")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("general.player-only"));
                    return true;
                }
                Player p = (Player) sender;
                if (!p.hasPermission("skyblock.reforge")) {
                    p.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                new dev.agam.skyblockitems.reforge.gui.ReforgeVIPGUI(plugin, p).open();
                return true;
            }
        }

        // 2. Open VIP Reforge GUI (Default for /reforge)
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("skyblock.reforge")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        new dev.agam.skyblockitems.reforge.gui.ReforgeVIPGUI(plugin, player).open();
        return true;
    }
}
