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
        // Handle reload subcommand for admins
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("skyblock.admin")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                return true;
            }

            plugin.getReforgeManager().reload();
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload-success"));
            return true;
        }

        // Player-only command
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("skyblock.reforge")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        // Open the reforge GUI
        new ReforgeGUI(plugin, player).open();

        return true;
    }
}
