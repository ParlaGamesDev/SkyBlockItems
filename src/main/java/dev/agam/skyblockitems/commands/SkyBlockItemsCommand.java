package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkyBlockItemsCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skyblockitems.admin")) {
            sender.sendMessage(ChatColor.RED + "אין לך הרשאה לפקודה הזאת!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            SkyBlockItems.getInstance().reloadAllConfigs();
            sender.sendMessage(ChatColor.GREEN + "[SkyBlockItems] Configuration reloaded successfully!");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "שימוש שגוי, נסה את הפקודה הזאת, /sbi reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("reload")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
