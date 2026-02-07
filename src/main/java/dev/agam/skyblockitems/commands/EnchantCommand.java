package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantingGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open the Enchanting GUI.
 * Usage: /enchant
 * Aliases: enchants, et
 * Permission: skyblock.enchant
 */
public class EnchantCommand implements CommandExecutor {

    private final SkyBlockItems plugin;

    public EnchantCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }

        if (!player.hasPermission("skyblock.enchant")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        new EnchantingGUI(plugin, player).open();
        return true;
    }
}
