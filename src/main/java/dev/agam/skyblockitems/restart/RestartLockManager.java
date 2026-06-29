package dev.agam.skyblockitems.restart;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles pre-restart safety: 60s countdown, /lobby for everyone, and global interaction lock.
 */
public class RestartLockManager implements Listener {

    private final SkyBlockItems plugin;

    private boolean enabled;
    private int countdownSeconds;
    private String lobbyCommand;
    private int reLobbyInterval;
    private boolean interceptRestartCommands;
    private List<String> restartCommands;
    private List<String> allowedCommands;
    private boolean kickBeforeShutdown;
    private int postKickWaitSeconds;
    private boolean saveVanillaPlayerData;
    private boolean pdsSaveDuringCountdown;
    private int pdsSaveWaitSeconds;

    private boolean globalLock;
    private final Set<UUID> lockedPlayers = Collections.synchronizedSet(new HashSet<>());
    private boolean restartPending;
    private boolean shuttingDown;
    private int secondsRemaining;
    private BukkitTask countdownTask;
    private BukkitTask reLobbyTask;
    private BukkitTask shutdownWaitTask;

    public RestartLockManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        reloadSettings();
    }

    public void reloadSettings() {
        enabled = plugin.getConfig().getBoolean("restart-lock.enabled", true);
        countdownSeconds = Math.max(1, plugin.getConfig().getInt("restart-lock.countdown-seconds", 60));
        lobbyCommand = plugin.getConfig().getString("restart-lock.lobby-command", "lobby");
        reLobbyInterval = Math.max(1, plugin.getConfig().getInt("restart-lock.re-lobby-interval", 5));
        interceptRestartCommands = plugin.getConfig().getBoolean("restart-lock.intercept-restart-commands", true);
        restartCommands = normalizeList(plugin.getConfig().getStringList("restart-lock.restart-commands"));
        if (restartCommands.isEmpty()) {
            restartCommands = List.of("stop", "restart", "minecraft:stop", "spigot:restart");
        }
        allowedCommands = normalizeList(plugin.getConfig().getStringList("restart-lock.allowed-commands"));
        if (allowedCommands.isEmpty()) {
            allowedCommands = List.of("lobby", "hub", "server", "lock");
        }
        kickBeforeShutdown = plugin.getConfig().getBoolean("restart-lock.kick-before-shutdown", true);
        postKickWaitSeconds = Math.max(1, plugin.getConfig().getInt("restart-lock.post-kick-wait-seconds", 8));
        saveVanillaPlayerData = plugin.getConfig().getBoolean("restart-lock.save-vanilla-playerdata", true);
        pdsSaveDuringCountdown = plugin.getConfig().getBoolean("restart-lock.pds-save-during-countdown", true);
        pdsSaveWaitSeconds = Math.max(1, plugin.getConfig().getInt("restart-lock.pds-save-wait-seconds", 15));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isGlobalLockActive() {
        return globalLock;
    }

    public boolean isRestartPending() {
        return restartPending;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public boolean isInteractionLocked(Player player) {
        if (player == null) {
            return false;
        }
        return globalLock || lockedPlayers.contains(player.getUniqueId());
    }

    public void lockAllOnlinePlayers() {
        globalLock = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            lockPlayer(player);
        }
    }

    public void lockPlayer(Player player) {
        if (player == null) {
            return;
        }
        lockedPlayers.add(player.getUniqueId());
        player.closeInventory();
    }

    public void unlockAll() {
        if (restartPending) {
            return;
        }
        globalLock = false;
        lockedPlayers.clear();
    }

    public boolean beginRestartCountdown(CommandSender initiator) {
        if (!enabled || restartPending) {
            return false;
        }

        restartPending = true;
        secondsRemaining = countdownSeconds;
        lockAllOnlinePlayers();
        flushPdsThen(this::sendLobbyCommands);
        startReLobbyTask();
        startCountdownTask();
        return true;
    }

    public boolean cancelRestartCountdown(CommandSender initiator) {
        if (!restartPending) {
            return false;
        }
        stopTasks();
        restartPending = false;
        shuttingDown = false;
        secondsRemaining = 0;
        globalLock = false;
        lockedPlayers.clear();
        return true;
    }

    public void sendAllToLobby() {
        flushPdsThen(this::sendLobbyCommands);
    }

    private void sendLobbyCommands() {
        String cmd = lobbyCommand == null ? "lobby" : lobbyCommand.trim();
        if (cmd.isEmpty()) {
            cmd = "lobby";
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            lockPlayer(player);
            player.performCommand(cmd);
        }
    }

    private void flushPdsThen(Runnable next) {
        if (!pdsSaveDuringCountdown || !dev.agam.skyblockitems.integration.PlayerDataSyncHook.isAvailable()) {
            next.run();
            return;
        }
        dev.agam.skyblockitems.integration.PlayerDataSyncHook.forceSaveAllAndAwait(
                plugin, pdsSaveWaitSeconds, next);
    }

    private void startCountdownTask() {
        stopCountdownTask();
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!restartPending) {
                stopCountdownTask();
                return;
            }

            if (secondsRemaining <= 0) {
                finishRestart();
                return;
            }

            secondsRemaining--;
            if (secondsRemaining <= 0) {
                finishRestart();
            }
        }, 20L, 20L);
    }

    private void startReLobbyTask() {
        stopReLobbyTask();
        reLobbyTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!restartPending) {
                stopReLobbyTask();
                return;
            }
            sendAllToLobby();
        }, reLobbyInterval * 20L, reLobbyInterval * 20L);
    }

    private void finishRestart() {
        if (shuttingDown) {
            return;
        }
        shuttingDown = true;
        stopTasks();
        restartPending = false;

        if (kickBeforeShutdown) {
            dev.agam.skyblockitems.integration.PlayerDataSyncHook.forceSaveAllAndAwait(
                    plugin, pdsSaveWaitSeconds, () -> {
                        kickAllPlayers();
                        startShutdownWaitTask();
                    });
            return;
        }

        dev.agam.skyblockitems.integration.PlayerDataSyncHook.forceSaveAllAndAwait(
                plugin, pdsSaveWaitSeconds, this::executeShutdown);
    }

    private void kickAllPlayers() {
        for (Player player : new java.util.ArrayList<>(Bukkit.getOnlinePlayers())) {
            player.kickPlayer("");
        }
    }

    private void startShutdownWaitTask() {
        stopShutdownWaitTask();
        final int[] elapsedSeconds = {0};
        shutdownWaitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            elapsedSeconds[0]++;
            if (Bukkit.getOnlinePlayers().isEmpty() || elapsedSeconds[0] >= postKickWaitSeconds) {
                stopShutdownWaitTask();
                executeShutdown();
            }
        }, 20L, 20L);
    }

    private void executeShutdown() {
        if (saveVanillaPlayerData) {
            Bukkit.getServer().savePlayers();
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                world.save();
            }
        }

        Bukkit.shutdown();
    }

    private void stopTasks() {
        stopCountdownTask();
        stopReLobbyTask();
        stopShutdownWaitTask();
    }

    private void stopCountdownTask() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void stopReLobbyTask() {
        if (reLobbyTask != null) {
            reLobbyTask.cancel();
            reLobbyTask = null;
        }
    }

    private void stopShutdownWaitTask() {
        if (shutdownWaitTask != null) {
            shutdownWaitTask.cancel();
            shutdownWaitTask = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (!enabled || !interceptRestartCommands || restartPending) {
            return;
        }
        String raw = event.getCommand().trim();
        if (!isRestartCommand(raw)) {
            return;
        }
        event.setCancelled(true);
        beginRestartCountdown(event.getSender());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message == null || message.isEmpty()) {
            return;
        }

        String raw = message.startsWith("/") ? message.substring(1) : message;
        String baseCommand = raw.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);

        if (interceptRestartCommands && !restartPending && enabled && isRestartCommand(raw)) {
            if (player.hasPermission("skyblockitems.admin.restart")) {
                event.setCancelled(true);
                beginRestartCountdown(player);
            }
            return;
        }

        if (!isInteractionLocked(player) && !restartPending) {
            return;
        }

        if (isAllowedDuringLock(baseCommand)) {
            return;
        }

        event.setCancelled(true);
    }

    public boolean isAllowedDuringLock(String command) {
        if (command == null) {
            return false;
        }
        String normalized = command.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        if (normalized.startsWith("bukkit:")) {
            normalized = normalized.substring("bukkit:".length());
        }
        for (String allowed : allowedCommands) {
            if (normalized.equals(allowed)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRestartCommand(String command) {
        String normalized = command.toLowerCase(Locale.ROOT).trim();
        String base = normalized.split("\\s+", 2)[0];
        for (String restartCmd : restartCommands) {
            if (base.equals(restartCmd)) {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizeList(List<String> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        return input.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toList());
    }
}
