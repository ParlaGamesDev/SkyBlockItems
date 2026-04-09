package dev.agam.skyblockitems.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Utility class for WorldGuard integration.
 */
public class WorldGuardUtils {

    private static boolean worldGuardEnabled = false;

    static {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            worldGuardEnabled = true;
        } catch (ClassNotFoundException e) {
            worldGuardEnabled = false;
        }
    }

    /**
     * Checks if a player can break a block at the given location according to WorldGuard.
     *
     * @param player   The player to check.
     * @param location The location of the block.
     * @return True if the player can break the block, false otherwise.
     */
    public static boolean canBreakBlock(Player player, Location location) {
        if (!worldGuardEnabled) {
            return true; // Assume permitted if WorldGuard is missing
        }

        try {
            LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            
            // testBuild checks many things including build flag and other protections
            return query.testBuild(BukkitAdapter.adapt(location), localPlayer, Flags.BLOCK_BREAK);
        } catch (NoClassDefFoundError | Exception e) {
            // Fallback for safety or version mismatches
            return true; 
        }
    }
}
