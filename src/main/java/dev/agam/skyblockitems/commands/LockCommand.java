package dev.agam.skyblockitems.commands;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.restart.RestartLockManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LockCommand implements CommandExecutor, TabCompleter {

    private final SkyBlockItems plugin;

    public LockCommand(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        RestartLockManager manager = plugin.getRestartLockManager();
        if (manager == null || !sender.hasPermission("skyblockitems.admin.lock")) {
            return true;
        }

        String sub = args.length == 0 ? "on" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "on", "start", "lock" -> manager.lockAllOnlinePlayers();
            case "off", "stop", "unlock" -> {
                if (!manager.isRestartPending()) {
                    manager.unlockAll();
                }
            }
            case "restart" -> manager.beginRestartCountdown(sender);
            case "cancel" -> manager.cancelRestartCountdown(sender);
            case "lobby" -> manager.sendAllToLobby();
            default -> {
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("skyblockitems.admin.lock")) {
            return List.of();
        }
        if (args.length == 1) {
            return Arrays.asList("on", "off", "restart", "cancel", "lobby").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
