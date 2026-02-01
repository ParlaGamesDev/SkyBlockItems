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
            String msg = SkyBlockItems.getInstance().getMessagesConfig().getString("staff.no-permission",
                    "&cאין לך הרשאה לבצע פעולה זו!");
            if (sender instanceof org.bukkit.entity.Player) {
                dev.agam.skyblockitems.utils.MessageUtils.sendMessage((org.bukkit.entity.Player) sender, msg);
            } else {
                sender.sendMessage(dev.agam.skyblockitems.utils.ColorUtils.translate(msg));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            SkyBlockItems.getInstance().reloadAllConfigs();
            String msg = SkyBlockItems.getInstance().getMessagesConfig().getString("staff.plugin-reloaded",
                    "&a[SkyBlockItems] הקונפיגורציה נטענה מחדש בהצלחה!");
            if (sender instanceof org.bukkit.entity.Player) {
                dev.agam.skyblockitems.utils.MessageUtils.sendMessage((org.bukkit.entity.Player) sender, msg);
            } else {
                sender.sendMessage(dev.agam.skyblockitems.utils.ColorUtils.translate(msg));
            }
            return true;
        }

        String msg = SkyBlockItems.getInstance().getMessagesConfig().getString("staff.wrong-usage",
                "&cשימוש שגוי, נסה את הפקודה הזאת, /sbi reload");
        if (sender instanceof org.bukkit.entity.Player) {
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage((org.bukkit.entity.Player) sender, msg);
        } else {
            sender.sendMessage(dev.agam.skyblockitems.utils.ColorUtils.translate(msg));
        }
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
