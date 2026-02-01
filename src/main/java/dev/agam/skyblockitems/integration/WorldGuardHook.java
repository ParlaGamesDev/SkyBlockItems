package dev.agam.skyblockitems.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardHook {

    public static StateFlag ABILITIES_FLAG;
    private static boolean enabled = false;

    /**
     * MUST be called during onLoad() of the plugin.
     */
    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("skyblock-abilities", true);
            registry.register(flag);
            ABILITIES_FLAG = flag;
            enabled = true;
        } catch (FlagConflictException e) {
            com.sk89q.worldguard.protection.flags.Flag<?> existing = registry.get("skyblock-abilities");
            if (existing instanceof StateFlag)
                ABILITIES_FLAG = (StateFlag) existing;
            enabled = true;
        } catch (Exception ignored) {
        }
    }

    public static boolean isAbilitiesEnabled(Player player, Location loc) {
        return testState(player, loc, ABILITIES_FLAG);
    }

    private static boolean testState(Player player, Location loc, StateFlag flag) {
        if (!enabled || flag == null)
            return true;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        return query.testState(BukkitAdapter.adapt(loc),
                com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
