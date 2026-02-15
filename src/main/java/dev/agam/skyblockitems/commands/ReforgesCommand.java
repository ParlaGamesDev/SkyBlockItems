package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.reforge.gui.ReforgeListGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin command to manage reforges.
 * Usage: /reforges
 * Permission: skyblockitems.reforges
 */
public class ReforgesCommand implements CommandExecutor {

    private final SkyBlockItems plugin;

    public ReforgesCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }

        if (!player.hasPermission("skyblockitems.reforges")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        new ReforgeListGUI(plugin, player).open();
        return true;
    }
}
