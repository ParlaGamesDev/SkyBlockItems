package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantEditorGUI;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantListGUI;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantingGUI;
import dev.agam.skyblockitems.enchantsystem.gui.CustomAnvilGUI;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }

        // This command requires admin permission
        if (!player.hasPermission("skyblock.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.sbi.usage",
                    "{usage}", "/sbi <reload|givebook|blacklist|admin|edit>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                plugin.reloadAllConfigs();
                player.sendMessage(plugin.getConfigManager().getMessage("commands.reload-success"));
            }

            case "givebook" -> {
                handleGiveBook(player, args);
            }

            case "blacklist" -> {
                handleBlacklist(player, args);
            }

            case "admin" -> {
                new EnchantListGUI(plugin, player).open();
            }

            case "edit" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.edit.usage"));
                    return true;
                }
                CustomEnchant enchant = plugin.getCustomEnchantManager().getEnchant(args[1]);
                if (enchant == null) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.edit.not-found")
                            .replace("{id}", args[1]));
                    return true;
                }
                new EnchantEditorGUI(plugin, player, enchant).open();
            }

            default -> {
                player.sendMessage(plugin.getConfigManager().getMessage("commands.unknown"));
            }
        }

        return true;
    }

    private void handleGiveBook(Player player, String[] args) {
        if (!player.hasPermission("skyblockitems.give") && !player.isOp()) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return;
        }
        if (args.length < 4) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.usage"));
            return;
        }
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.player-not-found",
                    "{player}", args[1]));
            return;
        }

        String enchantId = args[2];
        int bookLevel;
        try {
            bookLevel = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.invalid-level"));
            return;
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
            return;
        }

        targetPlayer.getInventory().addItem(bookToGive);
        player.sendMessage(plugin.getConfigManager().getMessage("commands.givebook.success",
                "{enchant}", displayName,
                "{level}", String.valueOf(bookLevel),
                "{player}", targetPlayer.getName()));
    }

    private void handleBlacklist(Player player, String[] args) {
        if (!player.hasPermission("skyblockitems.blacklist")) {
            player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            return;
        }

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
                plugin.getConfigManager().addToBlacklist(item.getType().name());
                player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.added")
                        .replace("{material}", item.getType().name()));
            }
            case "remove" -> {
                if (item == null || item.getType().isAir()) {
                    player.sendMessage(plugin.getConfigManager().getMessage("general.must-hold-item"));
                    return;
                }
                plugin.getConfigManager().removeFromBlacklist(item.getType().name());
                player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.removed")
                        .replace("{material}", item.getType().name()));
            }
            case "list" -> {
                List<String> blacklist = plugin.getConfigManager().getBlacklist();
                player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.list-header"));
                for (String s : blacklist) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.blacklist.list-item")
                            .replace("{material}", s));
                }
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("reload", "givebook", "blacklist", "admin", "edit");
            return filterCompletions(subs, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "givebook" -> {
                    return null; // Player names
                }
                case "blacklist" -> {
                    return filterCompletions(Arrays.asList("add", "remove", "list"), args[1]);
                }
                case "edit" -> {
                    for (CustomEnchant enchant : plugin.getCustomEnchantManager().getAllEnchants()) {
                        completions.add(enchant.getId());
                    }
                    return filterCompletions(completions, args[1]);
                }
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
