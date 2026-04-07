package dev.agam.skyblockitems.utils;

import org.bukkit.entity.Entity;

public class TargetUtils {

    /**
     * Checks if an entity is an NPC.
     * Supports Citizens and other plugins that use "NPC" metadata.
     */
    public static boolean isNPC(Entity entity) {
        if (entity == null) return false;
        
        // Citizens and most NPC plugins use the "NPC" metadata
        if (entity.hasMetadata("NPC")) return true;
        
        // Some plugins use this fixed metadata
        if (entity.hasMetadata("npc")) return true;
        
        // Citizens check by class name if available, but metadata is safer/standard
        return false;
    }
}
