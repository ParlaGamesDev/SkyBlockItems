package dev.agam.skyblockitems.enchantsystem.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantEditorGUI;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantListGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command handler for /mmoenchants
 */
public class MMOEnchantsCommand implements CommandExecutor, TabCompleter {

    private final SkyBlockItems plugin;

    public MMOEnchantsCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("errors.player-only"));
            return true;
        }

        if (!player.hasPermission("mmoenchants.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            // Open main GUI
            new EnchantListGUI(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.create-usage"));
                    return true;
                }
                String id = args[1].toLowerCase().replace(" ", "_");
                if (plugin.getCustomEnchantManager().exists(id)) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.already-exists"));
                    return true;
                }
                CustomEnchant enchant = plugin.getCustomEnchantManager().createEnchant(id);
                player.sendMessage(
                        plugin.getConfigManager().getMessage("commands.mmoenchants.created").replace("{id}", id));
                new EnchantEditorGUI(plugin, player, enchant).open();
            }
            case "edit" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.edit-usage"));
                    return true;
                }
                CustomEnchant enchant = plugin.getCustomEnchantManager().getEnchant(args[1]);
                if (enchant == null) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.not-found")
                            .replace("{id}", args[1]));
                    return true;
                }
                new EnchantEditorGUI(plugin, player, enchant).open();
            }
            case "delete" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.delete-usage"));
                    return true;
                }
                if (!plugin.getCustomEnchantManager().exists(args[1])) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.not-found")
                            .replace("{id}", args[1]));
                    return true;
                }
                plugin.getCustomEnchantManager().deleteEnchant(args[1]);
                player.sendMessage(
                        plugin.getConfigManager().getMessage("commands.mmoenchants.deleted").replace("{id}", args[1]));
            }
            case "list" -> {
                new EnchantListGUI(plugin, player).open();
            }
            case "reload" -> {
                plugin.getCustomEnchantManager().reload();
                player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.reloaded"));
            }
            case "help" -> {
                sendHelp(player);
            }
            default -> {
                player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.unknown"));
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.help.header"));
        player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.help.list"));
        player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.help.create"));
        player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.help.edit"));
        player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.help.delete"));
        player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.help.list-all"));
        player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.help.reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "edit", "delete", "list", "reload", "help"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("delete")) {
                for (CustomEnchant enchant : plugin.getCustomEnchantManager().getAllEnchants()) {
                    completions.add(enchant.getId());
                }
            }
        }

        String prefix = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        return completions;
    }
}
