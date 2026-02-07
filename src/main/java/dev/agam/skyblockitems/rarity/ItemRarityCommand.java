package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemRarityCommand implements CommandExecutor, org.bukkit.command.TabCompleter {

    private final SkyBlockItems plugin;

    public ItemRarityCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ir.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.getRarityManager().getConfigManager().loadConfigs();
            sender.sendMessage(ChatColor.GREEN + "ItemRarity configuration reloaded.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Player only command.");
            return true;
        }
        Player p = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) {
            p.sendMessage(ChatColor.RED + "Hold an item.");
            return true;
        }

        if (sub.equals("test")) {
            p.sendMessage(ChatColor.YELLOW + "--- Rarity Debug ---");
            p.sendMessage(ChatColor.GRAY + "Item: " + item.getType());
            String calculatedId = plugin.getRarityManager().getRarityId(item);
            p.sendMessage(ChatColor.GRAY + "Calculated Rarity ID: " + ChatColor.GOLD + calculatedId);
            plugin.getRarityManager().updateLore(item);
            p.sendMessage(ChatColor.GREEN + "Lore updated!");
            return true;
        }

        if (sub.equals("set")) {
            if (args.length < 2) {
                p.sendMessage(ChatColor.RED + "Usage: /ir set <rarity> [custom]");
                return true;
            }

            String rarityId = args[1].toUpperCase();
            boolean isNone = rarityId.equalsIgnoreCase("NONE");

            if (!isNone && plugin.getRarityManager().getConfigManager().getRarity(rarityId) == null) {
                p.sendMessage(ChatColor.RED + "Invalid rarity ID.");
                return true;
            }

            // /ir set <rarity> custom
            if (args.length > 2 && args[2].equalsIgnoreCase("custom")) {
                // Special case for NONE
                if (isNone) {
                    plugin.getRarityManager().getConfigManager().addNoRarityRule(item.getType());
                    p.sendMessage(ChatColor.GREEN + "Added 'No Rarity' rule for " + item.getType());
                    plugin.getRarityManager().updateLore(item); // Update immediately
                    return true;
                }

                List<String> currentLore = new java.util.ArrayList<>();
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    for (String line : item.getItemMeta().getLore()) {
                        if (!plugin.getRarityManager().isRarityLine(line)) {
                            currentLore.add(line);
                        }
                    }
                }

                plugin.getRarityManager().getConfigManager().addCustomRule(item.getType(), rarityId, currentLore);
                p.sendMessage(ChatColor.GREEN + "Added custom rule: " + item.getType() + " -> " + rarityId);
                p.sendMessage(ChatColor.GREEN + "Saved " + currentLore.size() + " lines of lore to config.");

                // Also update the item visually
                ItemStack newItem = plugin.getRarityManager().setRarity(item, rarityId);
                p.getInventory().setItemInMainHand(newItem);
                return true;
            }

            // /ir set <rarity>
            if (isNone) {
                p.sendMessage(ChatColor.RED + "Cannot set NONE rarity directly without 'custom' rule.");
                return true;
            }

            ItemStack newItem = plugin.getRarityManager().setRarity(item, rarityId);
            p.getInventory().setItemInMainHand(newItem);
            p.sendMessage(ChatColor.GREEN + "Rarity set to " + rarityId);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "ItemRarity Commands:");
        sender.sendMessage(
                ChatColor.GRAY + "/ir set <rarity> [custom] " + ChatColor.WHITE + "- Set rarity (or add custom rule)");
        sender.sendMessage(ChatColor.GRAY + "/ir test " + ChatColor.WHITE + "- Debug item rarity");
        sender.sendMessage(ChatColor.GRAY + "/ir reload " + ChatColor.WHITE + "- Reload config");
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ir.admin"))
            return java.util.Collections.emptyList();

        if (args.length == 1) {
            return java.util.Arrays.asList("set", "test", "reload");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            List<String> list = plugin.getRarityManager().getConfigManager().getRarities().stream()
                    .map(r -> r.identifier)
                    .collect(java.util.stream.Collectors.toList());
            list.add("NONE");
            return list;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return java.util.Collections.singletonList("custom");
        }

        return java.util.Collections.emptyList();
    }
}
