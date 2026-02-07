package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.CustomAnvilGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the Custom Anvil GUI.
 * Usage: /anvil
 * Permission: skyblock.anvil
 */
public class AnvilCommand implements CommandExecutor {

    private final SkyBlockItems plugin;

    public AnvilCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }

        if (!player.hasPermission("skyblock.anvil")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        new CustomAnvilGUI(plugin, player).open();
        return true;
    }
}
