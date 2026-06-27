package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GiveCommand implements CommandExecutor, TabCompleter, Listener {

    private final SkyBlockItems plugin;

    public GiveCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.toLowerCase().startsWith("/give ") || message.toLowerCase().startsWith("/minecraft:give ")) {
            event.setCancelled(true);
            handleGiveCommand(event.getPlayer(), message.substring(1).split(" "));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConsoleCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        if (command.toLowerCase().startsWith("give ") || command.toLowerCase().startsWith("minecraft:give ")) {
            event.setCancelled(true);
            handleGiveCommand(event.getSender(), command.split(" "));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Fallback if somehow executed normally
        List<String> list = new ArrayList<>();
        list.add("give");
        list.addAll(Arrays.asList(args));
        handleGiveCommand(sender, list.toArray(new String[0]));
        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minecraft.command.give") && !sender.hasPermission("essentials.give")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /give <player> <item> [amount]");
            return;
        }

        String targetName = args[1];
        List<Player> targets = new ArrayList<>();

        if (targetName.equals("*") || targetName.equalsIgnoreCase("@a")) {
            targets.addAll(Bukkit.getOnlinePlayers());
            if (targets.isEmpty()) {
                sender.sendMessage("§cNo players online.");
                return;
            }
        } else {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) {
                sender.sendMessage("§cPlayer not found.");
                return;
            }
            targets.add(target);
        }

        String itemString = args[2];
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount.");
                return;
            }
        }

        ItemStack item = null;

        // Try using Paper's ItemFactory to parse modern NBT/components if it exists
        try {
            item = Bukkit.getItemFactory().createItemStack(itemString);
        } catch (Throwable t) {
            // Fallback to basic material matching if the method doesn't exist or syntax is wrong
            try {
                String cleanName = itemString;
                if (cleanName.contains("{")) {
                    cleanName = cleanName.substring(0, cleanName.indexOf("{"));
                }
                if (cleanName.contains("[")) {
                    cleanName = cleanName.substring(0, cleanName.indexOf("["));
                }
                if (cleanName.startsWith("minecraft:")) {
                    cleanName = cleanName.substring(10);
                }
                Material mat = Material.matchMaterial(cleanName);
                if (mat != null) {
                    item = new ItemStack(mat);
                }
            } catch (Exception e) {}
        }

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage("§cUnknown item: " + itemString);
            return;
        }

        item.setAmount(amount);

        // Apply rarity + lore on the item itself (not packet-only)
        item = plugin.getRarityManager().processItemWithLore(item);

        // Give the item to the players
        for (Player p : targets) {
            p.getInventory().addItem(item.clone());
        }
        
        if (targets.size() == 1) {
            sender.sendMessage("§fGave " + "§a" + amount + " " + item.getType().name() + "§f to §a" + targets.get(0).getName());
        } else {
            sender.sendMessage("§fGave " + "§a" + amount + " " + item.getType().name() + "§f to §a" + targets.size() + " players.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            List<String> materials = new ArrayList<>();
            for (Material mat : Material.values()) {
                if (mat.isItem()) {
                    materials.add(mat.name().toLowerCase());
                    materials.add("minecraft:" + mat.name().toLowerCase());
                }
            }
            return materials.stream()
                    .filter(name -> name.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
