package dev.agam.skyblockitems.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages ability cooldowns for players.
 * Stores cooldown end times and provides utility methods for checking and
 * setting cooldowns.
 */
public class CooldownManager {

    private static final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * Check if a player's ability is on cooldown.
     * 
     * @param playerUUID The player's UUID
     * @param abilityId  The ability ID
     * @return true if on cooldown, false otherwise
     */
    public static boolean isOnCooldown(UUID playerUUID, String abilityId) {
        if (!cooldowns.containsKey(playerUUID))
            return false;
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (!playerCooldowns.containsKey(abilityId))
            return false;
        return System.currentTimeMillis() < playerCooldowns.get(abilityId);
    }

    /**
     * Get remaining cooldown time in seconds.
     * 
     * @param playerUUID The player's UUID
     * @param abilityId  The ability ID
     * @return Remaining seconds, or 0 if not on cooldown
     */
    public static double getRemainingCooldown(UUID playerUUID, String abilityId) {
        if (!cooldowns.containsKey(playerUUID))
            return 0;
        Map<String, Long> playerCooldowns = cooldowns.get(playerUUID);
        if (!playerCooldowns.containsKey(abilityId))
            return 0;
        long remaining = playerCooldowns.get(abilityId) - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000.0 : 0;
    }

    /**
     * Set a cooldown for a player's ability.
     * 
     * @param playerUUID      The player's UUID
     * @param abilityId       The ability ID
     * @param cooldownSeconds The cooldown duration in seconds
     */
    public static void setCooldown(UUID playerUUID, String abilityId, double cooldownSeconds) {
        cooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>());
        long endTime = System.currentTimeMillis() + (long) (cooldownSeconds * 1000);
        cooldowns.get(playerUUID).put(abilityId, endTime);
    }

    /**
     * Clear all cooldowns for a player.
     * 
     * @param playerUUID The player's UUID
     */
    public static void clearCooldowns(UUID playerUUID) {
        cooldowns.remove(playerUUID);
    }

    /**
     * Clear a specific cooldown for a player.
     * 
     * @param playerUUID The player's UUID
     * @param abilityId  The ability ID
     */
    public static void clearCooldown(UUID playerUUID, String abilityId) {
        if (cooldowns.containsKey(playerUUID)) {
            cooldowns.get(playerUUID).remove(abilityId);
        }
    }
}
