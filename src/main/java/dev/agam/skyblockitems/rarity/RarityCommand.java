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
 * Command handler for /rarity commands.
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
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.usage")));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set" -> handleSet(sender, args);
            case "custom" -> handleCustom(sender, args);
            case "remove" -> handleRemove(sender);
            default -> sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.usage")));
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.player-only")));
            return;
        }

        if (!player.hasPermission("skyblockitems.rarity.set")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.set-usage")));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.no-item")));
            return;
        }

        String rarityId = args[1];
        Rarity rarity = rarityManager.getRarity(rarityId);

        if (rarity == null) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("rarity.invalid-rarity").replace("{id}", rarityId)));
            return;
        }

        // Apply rarity globally (saves to config + refreshes online players)
        rarityManager.saveMapping(item, rarity.getIdentifier());

        player.sendMessage(ColorUtils.colorize(
                plugin.getConfigManager().getMessage("rarity.set-success")
                        .replace("{rarity}", ColorUtils.colorize(rarity.getDisplayName()))));
    }

    /**
     * Handles /rarity custom <rarity_id> - marks item as custom so it won't be
     * auto-assigned.
     */
    private void handleCustom(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.player-only")));
            return;
        }

        if (!player.hasPermission("skyblockitems.rarity.set")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.custom-usage")));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.no-item")));
            return;
        }

        String rarityId = args[1];
        Rarity rarity = rarityManager.getRarity(rarityId);

        if (rarity == null) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("rarity.invalid-rarity").replace("{id}", rarityId)));
            return;
        }

        // Apply rarity globally (saves to config + refreshes online players)
        rarityManager.saveMapping(item, rarity.getIdentifier());

        player.sendMessage(ColorUtils.colorize(
                plugin.getConfigManager().getMessage("rarity.custom-success")
                        .replace("{rarity}", ColorUtils.colorize(rarity.getDisplayName()))));
    }

    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.player-only")));
            return;
        }

        if (!player.hasPermission("skyblockitems.rarity.remove")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("general.no-permission")));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.no-item")));
            return;
        }

        // Remove mapping globally (removes from config + refreshes online players)
        rarityManager.removeMapping(item);

        // Update current item in hand immediately for the caller
        // REMOVED: refreshPlayer inside removeMapping already handles this.
        // Calling processItem(item) here passes the STALE item (with old NBT) forcing a
        // re-save.

        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("rarity.removed")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = List.of("set", "custom", "remove");
            String input = args[0].toLowerCase();
            return subCommands.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("custom"))) {
            String input = args[1].toLowerCase();
            return rarityManager.getAllRarities().stream()
                    .map(Rarity::getIdentifier)
                    .filter(id -> id.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
