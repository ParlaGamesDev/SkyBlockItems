package dev.agam.skyblockitems.integration;

import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Forces PlayerDataSyncReloaded to persist online player data and waits for DB writes.
 */
public final class PlayerDataSyncHook {

    private static final String RELOADED_PLUGIN = "PlayerDataSyncReloaded";
    private static final String BUKKIT_PDS_PLAYER =
            "de.craftingstudiopro.playerDataSyncReloaded.plugin.BukkitPDSPlayer";
    private static final String PDS_PLAYER =
            "de.craftingstudiopro.playerDataSyncReloaded.api.PDSPlayer";
    private static final String PLAYER_DATA =
            "de.craftingstudiopro.playerDataSyncReloaded.api.PlayerData";

    private PlayerDataSyncHook() {
    }

    public static boolean isAvailable() {
        return findReloadedPlugin() != null;
    }

    public static void forceSaveAllAndAwait(SkyBlockItems plugin, int timeoutSeconds, Runnable onComplete) {
        Runnable completeOnMain = () -> Bukkit.getScheduler().runTask(plugin, onComplete);

        Plugin pds = findReloadedPlugin();
        if (pds == null) {
            completeOnMain.run();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<CompletableFuture<Void>> futures;
            try {
                futures = collectForcedSaveFutures(pds);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.FINE, "PlayerDataSyncReloaded direct save unavailable, using handleQuit fallback", t);
                if (!triggerHandleQuitFallback(pds)) {
                    dispatchSaveAllCommand();
                }
                completeOnMain.run();
                return;
            }

            if (futures.isEmpty()) {
                completeOnMain.run();
                return;
            }

            CompletableFuture<Void> combined = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                int timeout = Math.max(1, timeoutSeconds);
                try {
                    combined.get(timeout, TimeUnit.SECONDS);
                    plugin.getLogger().fine("PlayerDataSyncReloaded flushed " + futures.size() + " player(s).");
                } catch (TimeoutException e) {
                    plugin.getLogger().fine("PlayerDataSyncReloaded save timed out after " + timeout + "s.");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.FINE, "PlayerDataSyncReloaded save wait failed", e);
                }
                completeOnMain.run();
            });
        });
    }

    private static List<CompletableFuture<Void>> collectForcedSaveFutures(Plugin pds)
            throws ReflectiveOperationException {
        Object syncManager = resolveSyncManager(pds);
        Object storage = readField(syncManager, "storage");
        Object versionHandler = readField(syncManager, "versionHandler");

        Class<?> pdsPlayerClass = loadPdsClass(pds, PDS_PLAYER);
        Class<?> playerDataClass = loadPdsClass(pds, PLAYER_DATA);

        Method capture = versionHandler.getClass().getMethod("capture", pdsPlayerClass);
        Method filterData = syncManager.getClass().getDeclaredMethod("filterData", playerDataClass);
        filterData.setAccessible(true);
        Method save = storage.getClass().getMethod("save", playerDataClass);

        Constructor<?> pdsPlayerCtor = resolvePdsPlayerConstructor(pds);
        Set<String> excludedWorlds = readExcludedWorldsFromConfig(pds);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            if (player == null || isWorldExcluded(excludedWorlds, player.getWorld().getName())) {
                continue;
            }

            Object pdsPlayer = pdsPlayerCtor.newInstance(player);
            Object data = capture.invoke(versionHandler, pdsPlayer);
            filterData.invoke(syncManager, data);

            @SuppressWarnings("unchecked")
            CompletableFuture<Void> future = (CompletableFuture<Void>) save.invoke(storage, data);
            futures.add(future);
        }
        return futures;
    }

    private static boolean triggerHandleQuitFallback(Plugin pds) {
        try {
            Object syncManager = resolveSyncManager(pds);
            Constructor<?> pdsPlayerCtor = resolvePdsPlayerConstructor(pds);
            Class<?> pdsPlayerClass = loadPdsClass(pds, PDS_PLAYER);
            Method handleQuit = resolveHandleQuitMethod(syncManager, pdsPlayerClass);

            for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                if (player == null) {
                    continue;
                }
                Object pdsPlayer = pdsPlayerCtor.newInstance(player);
                if (handleQuit.getParameterCount() == 2) {
                    handleQuit.invoke(syncManager, pdsPlayer, false);
                } else {
                    handleQuit.invoke(syncManager, pdsPlayer);
                }
            }
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    private static Method resolveHandleQuitMethod(Object syncManager, Class<?> pdsPlayerClass)
            throws NoSuchMethodException {
        try {
            return syncManager.getClass().getMethod("handleQuit", pdsPlayerClass, boolean.class);
        } catch (NoSuchMethodException ignored) {
            return syncManager.getClass().getMethod("handleQuit", pdsPlayerClass);
        }
    }

    private static void dispatchSaveAllCommand() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playerdatasync saveall");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pds saveall");
    }

    private static Object resolveSyncManager(Plugin pds) throws ReflectiveOperationException {
        Method getSyncManager = pds.getClass().getMethod("getSyncManager");
        Object syncManager = getSyncManager.invoke(pds);
        if (syncManager == null) {
            throw new IllegalStateException("PlayerDataSyncReloaded sync manager is null");
        }
        return syncManager;
    }

    private static Class<?> loadPdsClass(Plugin pds, String className) throws ClassNotFoundException {
        ClassLoader loader = pds.getClass().getClassLoader();
        return Class.forName(className, true, loader);
    }

    private static Constructor<?> resolvePdsPlayerConstructor(Plugin pds) throws ReflectiveOperationException {
        Class<?> clazz = loadPdsClass(pds, BUKKIT_PDS_PLAYER);
        return clazz.getConstructor(Player.class);
    }

    private static Set<String> readExcludedWorldsFromConfig(Plugin pds) {
        Set<String> worlds = new HashSet<>();
        if (!(pds instanceof JavaPlugin javaPlugin)) {
            return worlds;
        }
        FileConfiguration config = javaPlugin.getConfig();
        if (config == null) {
            return worlds;
        }
        for (String world : config.getStringList("exclusions.worlds")) {
            if (world != null && !world.isBlank()) {
                worlds.add(world.trim().toLowerCase(Locale.ROOT));
            }
        }
        return worlds;
    }

    private static Object readField(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static boolean isWorldExcluded(Set<String> excludedWorlds, String worldName) {
        if (excludedWorlds == null || excludedWorlds.isEmpty() || worldName == null) {
            return false;
        }
        return excludedWorlds.contains(worldName.trim().toLowerCase(Locale.ROOT));
    }

    private static Plugin findReloadedPlugin() {
        Plugin reloaded = Bukkit.getPluginManager().getPlugin(RELOADED_PLUGIN);
        if (reloaded != null && reloaded.isEnabled()) {
            return reloaded;
        }
        return null;
    }
}
