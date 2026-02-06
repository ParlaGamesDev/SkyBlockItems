package dev.agam.skyblockitems.enchantsystem.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantingGUI;
import dev.agam.skyblockitems.enchantsystem.gui.CustomAnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to open the enchanting GUI and other subcommands.
 */
public class EnchantCommand implements CommandExecutor, TabCompleter {

    private final SkyBlockItems plugin;

    public EnchantCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("reload", "blacklist", "givebook", "anvil");
            for (String s : subs) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase()))
                    completions.add(s);
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("givebook")) {
            return null; // Player names
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("givebook")) {
            // Merge IDs from both managers
            for (CustomEnchant e : plugin.getCustomEnchantManager().getAllEnchants()) {
                completions.add(e.getId());
            }
            for (String id : plugin.getEnchantManager().getEnchants().keySet()) {
                completions.add(id);
            }
            // Add all Vanilla Enchants to Tab Completion
            for (Enchantment e : Enchantment.values()) {
                completions.add(e.getKey().getKey().toLowerCase());
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("givebook")) {
            String id = args[2];
            int max = 10;

            // Check config enchants first
            dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig conf = plugin
                    .getEnchantManager().getEnchant(id);
            if (conf != null) {
                max = conf.getMaxLevel();
            } else {
                // Check custom enchants
                CustomEnchant custom = plugin.getCustomEnchantManager().getEnchant(id);
                if (custom != null) {
                    max = custom.getMaxLevel();
                }
            }

            for (int i = 1; i <= Math.min(max, 10); i++) {
                completions.add(String.valueOf(i));
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("blacklist")) {
            completions.add("add");
            completions.add("remove");
            completions.add("list");
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("errors.player-only"));
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "reload":
                    if (!player.hasPermission("skyblockitems.reload")) {
                        player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                        return true;
                    }
                    plugin.getConfigManager().reload();
                    plugin.getEnchantManager().reload();
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.reload-success"));
                    return true;

                case "givebook":
                    if (!player.hasPermission("skyblockitems.give") && !player.isOp()) {
                        player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                        return true;
                    }
                    if (args.length < 4) {
                        player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.usage"));
                        return true;
                    }
                    Player targetPlayer = Bukkit.getPlayer(args[1]);
                    if (targetPlayer == null) {
                        player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.player-not-found",
                                "{player}", args[1]));
                        return true;
                    }

                    String enchantId = args[2];
                    int bookLevel;
                    try {
                        bookLevel = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.invalid-level"));
                        return true;
                    }

                    ItemStack bookToGive = null;
                    String displayName = enchantId;

                    // Try config enchants first
                    dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig conf = plugin
                            .getEnchantManager().getEnchant(enchantId);
                    if (conf != null) {
                        bookToGive = plugin.getEnchantManager().createEnchantedBook(conf, bookLevel);
                        displayName = conf.getDisplayName();
                    } else {
                        // Try custom enchants
                        CustomEnchant custom = plugin.getCustomEnchantManager().getEnchant(enchantId);
                        if (custom != null) {
                            bookToGive = plugin.getEnchantManager().createEnchantedBook(custom, bookLevel);
                            displayName = custom.getDisplayName();
                        }
                    }

                    if (bookToGive == null) {
                        player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.enchant-not-found",
                                "{id}", enchantId));
                        return true;
                    }

                    targetPlayer.getInventory().addItem(bookToGive);
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.success",
                            "{enchant}", displayName,
                            "{level}", String.valueOf(bookLevel),
                            "{player}", targetPlayer.getName()));
                    return true;

                case "anvil":
                    new CustomAnvilGUI(plugin, player).open();
                    return true;

                case "blacklist":
                    if (!player.hasPermission("skyblockitems.blacklist")) {
                        player.sendMessage(plugin.getConfigManager().getMessage("errors.no-permission"));
                        return true;
                    }

                    if (args.length < 2) {
                        player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist-usage"));
                        return true;
                    }

                    String action = args[1].toLowerCase();
                    ItemStack item = player.getInventory().getItemInMainHand();

                    switch (action) {
                        case "add":
                            if (item == null || item.getType().isAir()) {
                                player.sendMessage(plugin.getConfigManager().getMessage("errors.must-hold-item"));
                                return true;
                            }
                            plugin.getConfigManager().addToBlacklist(item.getType().name());
                            player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.added")
                                    .replace("{material}", item.getType().name()));
                            break;
                        case "remove":
                            if (item == null || item.getType().isAir()) {
                                player.sendMessage(plugin.getConfigManager().getMessage("errors.must-hold-item"));
                                return true;
                            }
                            plugin.getConfigManager().removeFromBlacklist(item.getType().name());
                            player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.removed")
                                    .replace("{material}", item.getType().name()));
                            break;
                        case "list":
                            List<String> blacklist = plugin.getConfigManager().getBlacklist();
                            player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.list-header"));
                            for (String s : blacklist) {
                                player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.list-item")
                                        .replace("{material}", s));
                            }
                            break;
                        default:
                            player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.usage"));
                            break;
                    }
                    return true;
            }
        }

        // Open enchanting GUI if no valid subcommand is given
        new EnchantingGUI(plugin, player).open();
        return true;
    }
}
