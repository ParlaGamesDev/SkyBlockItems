package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantListGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main command for the SkyBlockItems plugin (/sbi).
 * Consolidates all item, ability, and enchantment commands.
 */
public class SkyBlockItemsCommand implements CommandExecutor, TabCompleter {

    private final SkyBlockItems plugin;

    public SkyBlockItemsCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0].toLowerCase() : "";
        
        // Subcommands that allow console
        if (sub.equals("reload") || sub.equals("message") || sub.equals("givebook") || sub.equals("givereforgegem")) {
            // Proceed with execution
        } else if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize("&cThis command can only be executed by a player."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.unknown"));
            return true;
        }

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("skyblockitems.admin.reload")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                plugin.reloadAllConfigs();
                sender.sendMessage(plugin.getConfigManager().getMessage("commands.reload-success"));
            }

            case "message" -> {
                if (!sender.hasPermission("skyblockitems.admin.message")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                handleMessage(sender, args);
            }

            case "givebook" -> {
                if (!sender.hasPermission("skyblockitems.admin.give")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                handleGiveBook(sender, args);
            }

            case "blacklist" -> {
                Player player = (Player) sender;
                if (!player.hasPermission("skyblockitems.admin.blacklist")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                handleBlacklist(player, args);
            }

            case "enchants" -> {
                Player player = (Player) sender;
                if (!player.hasPermission("skyblockitems.admin.enchants")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                new EnchantListGUI(plugin, player).open();
            }

            case "reforges" -> {
                Player player = (Player) sender;
                if (!player.hasPermission("skyblockitems.admin.reforges")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                new dev.agam.skyblockitems.reforge.gui.ReforgeListGUI(plugin, player).open();
            }

            case "givereforgegem" -> {
                if (!sender.hasPermission("skyblockitems.admin.reforge")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
                    return true;
                }
                handleGiveReforgeGem(sender, args);
            }

            case "rarity" -> {
                // Delegate to RarityCommand
                String[] rarityArgs = new String[args.length - 1];
                System.arraycopy(args, 1, rarityArgs, 0, args.length - 1);
                new dev.agam.skyblockitems.rarity.RarityCommand(plugin).onCommand(sender, command, label, rarityArgs);
            }

            default -> {
                sender.sendMessage(plugin.getConfigManager().getMessage("commands.unknown"));
            }
        }

        return true;
    }

    private void handleMessage(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /sbi message <player> <message...>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize("&cPlayer '" + args[1] + "' not found."));
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            sb.append(args[i]).append(" ");
        }
        
        target.sendMessage(ColorUtils.colorize(sb.toString().trim()));
    }

    private void handleGiveBook(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.usage"));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.player-not-found",
                    "{player}", args[1]));
            return;
        }

        String enchantId = args[2];
        int bookLevel;
        try {
            bookLevel = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.invalid-level"));
            return;
        }

        ItemStack bookToGive = null;
        String displayName = enchantId;

        dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig conf = plugin
                .getEnchantManager().getEnchant(enchantId);
        if (conf != null) {
            bookToGive = plugin.getEnchantManager().createEnchantedBook(conf, bookLevel);
            displayName = conf.getDisplayName();
        } else {
            CustomEnchant custom = plugin.getCustomEnchantManager().getEnchant(enchantId);
            if (custom != null) {
                bookToGive = plugin.getEnchantManager().createEnchantedBook(custom, bookLevel);
                displayName = custom.getDisplayName();
            }
        }

        if (bookToGive == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.enchant-not-found",
                    "{id}", enchantId));
            return;
        }

        targetPlayer.getInventory().addItem(bookToGive);
        sender.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.success",
                "{enchant}", displayName,
                "{level}", String.valueOf(bookLevel),
                "{player}", targetPlayer.getName()));
    }

    private void handleGiveReforgeGem(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /sbi givereforgegem <gemId> [player]"));
            return;
        }

        String gemId = args[1];
        Player target = null;
        if (args.length > 2) {
            target = Bukkit.getPlayer(args[2]);
        } else if (sender instanceof Player p) {
            target = p;
        }

        if (target == null) {
            sender.sendMessage(ColorUtils.colorize("&cPlayer not found."));
            return;
        }

        ItemStack gem = plugin.getReforgeManager().getGemItem(gemId);
        if (gem == null) {
            sender.sendMessage(ColorUtils.colorize("&cGem '" + gemId + "' not found in any reforge configuration."));
            return;
        }

        target.getInventory().addItem(gem);
        sender.sendMessage(ColorUtils.colorize("&aGave " + gemId + " to " + target.getName()));
    }

    private void handleBlacklist(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.usage"));
            return;
        }

        String action = args[1].toLowerCase();
        ItemStack item = player.getInventory().getItemInMainHand();

        switch (action) {
            case "add" -> {
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("general.must-hold-item"));
                    return;
                }
                boolean special = plugin.getConfigManager().isSpecialItem(item);
                plugin.getConfigManager().addToBlacklist(item);

                String name = (special && item.getItemMeta().hasDisplayName())
                        ? item.getItemMeta().getDisplayName()
                        : item.getType().name();
                if (special && !item.getItemMeta().hasDisplayName())
                    name += " (Unique)";

                player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.added")
                        .replace("{material}", name));
            }
            case "remove" -> {
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("general.must-hold-item"));
                    return;
                }
                plugin.getConfigManager().removeFromBlacklist(item);
                player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.removed")
                        .replace("{material}", item.getType().name()));
            }
            case "list" -> {
                List<String> blacklist = plugin.getConfigManager().getBlacklist();
                player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.list-header"));
                for (String s : blacklist) {
                    String display = s;
                    if (s.startsWith("SPECIFIC:")) {
                        String[] parts = s.split(":");
                        display = "§d[Specific] §f" + (parts.length > 1 ? parts[1] : s);
                    }
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.list-item")
                            .replace("{material}", display));
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("reload", "givebook", "blacklist", "enchants", "reforges", "rarity",
                    "givereforgegem", "message");
            return filterCompletions(subs, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "givebook", "message", "givereforgegem" -> {
                    return null; // Player names
                }
                case "blacklist" -> {
                    return filterCompletions(Arrays.asList("add", "remove", "list"), args[1]);
                }
                case "rarity" -> {
                    String[] rarityArgs = new String[args.length - 1];
                    System.arraycopy(args, 1, rarityArgs, 0, args.length - 1);
                    return new dev.agam.skyblockitems.rarity.RarityCommand(plugin).onTabComplete(sender, command, alias,
                            rarityArgs);
                }
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("rarity")) {
                String[] rarityArgs = new String[args.length - 1];
                System.arraycopy(args, 1, rarityArgs, 0, args.length - 1);
                return new dev.agam.skyblockitems.rarity.RarityCommand(plugin).onTabComplete(sender, command, alias,
                        rarityArgs);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("givebook")) {
            for (CustomEnchant e : plugin.getCustomEnchantManager().getAllEnchants()) {
                completions.add(e.getId());
            }
            for (String id : plugin.getEnchantManager().getEnchants().keySet()) {
                completions.add(id);
            }
            for (Enchantment e : Enchantment.values()) {
                completions.add(e.getKey().getKey().toLowerCase());
            }
            return filterCompletions(completions, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("givebook")) {
            String id = args[2];
            int max = 10;
            var conf = plugin.getEnchantManager().getEnchant(id);
            if (conf != null) {
                max = conf.getMaxLevel();
            } else {
                CustomEnchant custom = plugin.getCustomEnchantManager().getEnchant(id);
                if (custom != null) {
                    max = custom.getMaxLevel();
                }
            }
            for (int i = 1; i <= Math.min(max, 10); i++) {
                completions.add(String.valueOf(i));
            }
            return filterCompletions(completions, args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
