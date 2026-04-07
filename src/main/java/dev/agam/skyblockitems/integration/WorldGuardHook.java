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
    public static StateFlag GRAPPLING_HOOK_FLAG;
    public static StateFlag TREE_CAPITATOR_FLAG;
    private static boolean enabled = false;

    /**
     * MUST be called during onLoad() of the plugin.
     */
    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        
        ABILITIES_FLAG = registerFlag(registry, "sbi-ability", true);
        GRAPPLING_HOOK_FLAG = registerFlag(registry, "sbi-grappling-hook", true);
        TREE_CAPITATOR_FLAG = registerFlag(registry, "sbi-tree-capitator", true);
        
        enabled = true;
    }

    private static StateFlag registerFlag(FlagRegistry registry, String name, boolean defaultValue) {
        try {
            StateFlag flag = new StateFlag(name, defaultValue);
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            com.sk89q.worldguard.protection.flags.Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag) {
                return (StateFlag) existing;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static boolean isAbilitiesEnabled(Player player, Location loc) {
        return testState(player, loc, ABILITIES_FLAG);
    }

    public static boolean isGrapplingHookEnabled(Player player, Location loc) {
        return testState(player, loc, GRAPPLING_HOOK_FLAG);
    }

    public static boolean isTreeCapitatorEnabled(Player player, Location loc) {
        return testState(player, loc, TREE_CAPITATOR_FLAG);
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
