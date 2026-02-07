package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.agam.skyblockitems.rarity.RarityManager;

public class RarityCommand implements CommandExecutor {

    private final SkyBlockItems plugin;

    public RarityCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must hold an item!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("raritytest")) {
            NBTItem nbt = NBTItem.get(item);
            String currentTag = nbt.hasTag("skyblock.rarity") ? nbt.getString("skyblock.rarity")
                    : "None (Defaulting to COMMON)";

            player.sendMessage(ChatColor.YELLOW + "--- Rarity Debug ---");
            player.sendMessage(ChatColor.GRAY + "Item: " + item.getType());
            player.sendMessage(ChatColor.GRAY + "NBT Tag: " + ChatColor.WHITE + currentTag);

            String calculatedId = plugin.getRarityManager().getRarityId(item);
            player.sendMessage(ChatColor.GRAY + "Calculated Rarity ID: " + ChatColor.GOLD + calculatedId);

            plugin.getRarityManager().updateLore(item);
            player.sendMessage(ChatColor.GREEN + "Lore updated!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("setrarity")) {
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Usage: /setrarity <RARITY_ID>");
                return true;
            }

            String rarityId = args[0].toUpperCase();
            if (plugin.getRarityManager().getConfigManager().getRarity(rarityId) == null) {
                player.sendMessage(ChatColor.RED + "Invalid rarity ID! Available: " +
                        String.join(", ", plugin.getRarityManager().getConfigManager().getRarities().stream()
                                .map(r -> r.identifier).toArray(String[]::new)));
                return true;
            }

            ItemStack newItem = plugin.getRarityManager().setRarity(item, rarityId);
            player.getInventory().setItemInMainHand(newItem);
            player.sendMessage(ChatColor.GREEN + "Rarity set to " + rarityId + " and lore updated!");
            return true;
        }

        return false;
    }
}
