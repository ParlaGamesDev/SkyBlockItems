package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command handler for /sbi rarity subcommands.
 */
public class RarityCommand implements CommandExecutor, TabCompleter {

    private final SkyBlockItems plugin;
    private final RarityManager rarityManager;

    public RarityCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.rarityManager = plugin.getRarityManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "custom" -> handleCustom(sender, args);
            case "remove" -> handleRemove(sender);
            default -> {
            }
        }

        return true;
    }

    private boolean isValidAssignableRarity(Rarity rarity) {
        return rarity != null && !rarity.getIdentifier().equalsIgnoreCase("NONE");
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.player-only")));
            return;
        }

        if (!player.hasPermission("skyblockitems.admin.rarity")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        if (args.length < 2) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return;
        }

        Rarity rarity = rarityManager.getRarity(args[1]);
        if (!isValidAssignableRarity(rarity)) {
            return;
        }

        rarityManager.saveTypeMapping(item, rarity.getIdentifier());

        ItemStack processed = rarityManager.reprocessItem(item);
        player.getInventory().setItemInMainHand(processed);
        player.updateInventory();
    }

    private void handleCustom(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.player-only")));
            return;
        }

        if (!player.hasPermission("skyblockitems.admin.rarity")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        if (args.length < 2) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return;
        }

        Rarity rarity = rarityManager.getRarity(args[1]);
        if (!isValidAssignableRarity(rarity)) {
            return;
        }

        ItemStack processed = rarityManager.applyRarity(item, rarity, true);
        player.getInventory().setItemInMainHand(processed);
        player.updateInventory();
    }

    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.player-only")));
            return;
        }

        if (!player.hasPermission("skyblockitems.admin.rarity")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            return;
        }

        ItemStack processed;
        if (rarityManager.hasCustomRarity(item)) {
            processed = rarityManager.removeRarity(item, true);
        } else {
            rarityManager.removeTypeMapping(item);
            processed = rarityManager.reprocessItem(item);
        }

        player.getInventory().setItemInMainHand(processed);
        player.updateInventory();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return List.of("set", "custom", "remove").stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("custom"))) {
            String input = args[1].toLowerCase();
            return rarityManager.getAllRarities().stream()
                    .map(Rarity::getIdentifier)
                    .filter(id -> !id.equalsIgnoreCase("NONE"))
                    .filter(id -> id.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
