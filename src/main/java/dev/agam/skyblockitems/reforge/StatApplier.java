package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Handles intelligent stat application to items.
 * Supports MMOItems stats and AuraSkills stat modifiers.
 */
public class StatApplier {

    private final SkyBlockItems plugin;

    // Combat stats - only for weapons
    private static final Set<String> COMBAT_STATS = Set.of(
            "mmoitems_attack_damage",
            "mmoitems_critical_strike_chance",
            "mmoitems_critical_strike_power",
            "auraskills_strength",
            "auraskills_crit_chance",
            "auraskills_crit_damage");

    // Tool stats - only for tools
    private static final Set<String> TOOL_STATS = Set.of(
            "mmoitems_mining_speed",
            "mmoitems_block_power",
            "auraskills_luck",
            "auraskills_fortune");

    // Combat item types
    private static final Set<String> COMBAT_TYPES = Set.of(
            "SWORD", "BOW", "CROSSBOW", "TRIDENT", "MACE");

    // Tool item types
    private static final Set<String> TOOL_TYPES = Set.of(
            "PICKAXE", "AXE", "SHOVEL", "HOE");

    public StatApplier(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies stats from a reforge to an item.
     * Uses smart lore display when stats already exist.
     * 
     * @param item     The item to apply stats to
     * @param reforge  The reforge containing stats
     * @param itemType The item type (for validation)
     */
    public void applyStats(ItemStack item, Reforge reforge, String itemType) {
        if (item == null || reforge == null) {
            return;
        }

        Map<String, Double> stats = reforge.getStats();
        if (stats.isEmpty()) {
            return;
        }

        NBTItem nbtItem = NBTItem.get(item);

        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            String statKey = entry.getKey();
            double statValue = entry.getValue();

            // Validate stat is appropriate for item type
            if (!isStatValidForItemType(statKey, itemType)) {
                plugin.getLogger().fine("Skipping stat " + statKey + " for item type " + itemType);
                continue;
            }

            // Apply the stat based on its type
            if (statKey.startsWith("mmoitems_")) {
                applyMMOItemsStat(nbtItem, statKey, statValue);
            } else if (statKey.startsWith("auraskills_")) {
                applyAuraSkillsStat(nbtItem, statKey, statValue);
            }
        }

        // Update the item with modified NBT
        item.setItemMeta(nbtItem.toItem().getItemMeta());
    }

    /**
     * Checks if a stat is valid for the given item type.
     */
    private boolean isStatValidForItemType(String stat, String itemType) {
        // Universal stats are always valid
        if (!COMBAT_STATS.contains(stat) && !TOOL_STATS.contains(stat)) {
            return true;
        }

        // Combat stats only for combat items
        if (COMBAT_STATS.contains(stat)) {
            return COMBAT_TYPES.contains(itemType.toUpperCase());
        }

        // Tool stats only for tools
        if (TOOL_STATS.contains(stat)) {
            return TOOL_TYPES.contains(itemType.toUpperCase());
        }

        return true;
    }

    /**
     * Applies an MMOItems stat.
     */
    private void applyMMOItemsStat(NBTItem nbtItem, String statKey, double value) {
        // Remove "mmoitems_" prefix to get the actual stat name
        String mmoStat = statKey.substring("mmoitems_".length()).toUpperCase().replace("_", "-");

        try {
            // Check if the item already has this stat
            String nbtKey = "MMOITEMS_" + mmoStat;

            if (nbtItem.hasTag(nbtKey)) {
                // Stat exists - add to it
                double currentValue = nbtItem.getDouble(nbtKey);
                nbtItem.addTag(new ItemTag(nbtKey, currentValue + value));
            } else {
                // New stat - just set it
                nbtItem.addTag(new ItemTag(nbtKey, value));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply MMOItems stat: " + mmoStat + " - " + e.getMessage());
        }
    }

    /**
     * Applies an AuraSkills stat modifier.
     */
    private void applyAuraSkillsStat(NBTItem nbtItem, String statKey, double value) {
        // Remove "auraskills_" prefix
        String auraStat = statKey.substring("auraskills_".length());

        try {
            // AuraSkills uses attribute modifiers stored in NBT
            // We'll use a custom NBT tag for reforge bonuses
            String nbtKey = "REFORGE_" + auraStat.toUpperCase();

            if (nbtItem.hasTag(nbtKey)) {
                double currentValue = nbtItem.getDouble(nbtKey);
                nbtItem.addTag(new ItemTag(nbtKey, currentValue + value));
            } else {
                nbtItem.addTag(new ItemTag(nbtKey, value));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply AuraSkills stat: " + auraStat + " - " + e.getMessage());
        }
    }

    /**
     * Removes all reforge stats from an item.
     * 
     * @param item    The item to remove stats from
     * @param reforge The reforge whose stats should be removed
     */
    public void removeStats(ItemStack item, Reforge reforge) {
        if (item == null || reforge == null) {
            return;
        }

        NBTItem nbtItem = NBTItem.get(item);
        Map<String, Double> stats = reforge.getStats();

        for (String statKey : stats.keySet()) {
            if (statKey.startsWith("mmoitems_")) {
                String mmoStat = statKey.substring("mmoitems_".length()).toUpperCase().replace("_", "-");
                String nbtKey = "MMOITEMS_" + mmoStat;

                if (nbtItem.hasTag(nbtKey)) {
                    double currentValue = nbtItem.getDouble(nbtKey);
                    double reforgeValue = stats.get(statKey);
                    double newValue = currentValue - reforgeValue;

                    if (newValue <= 0) {
                        nbtItem.removeTag(nbtKey);
                    } else {
                        nbtItem.addTag(new ItemTag(nbtKey, newValue));
                    }
                }
            } else if (statKey.startsWith("auraskills_")) {
                String auraStat = statKey.substring("auraskills_".length());
                String nbtKey = "REFORGE_" + auraStat.toUpperCase();
                nbtItem.removeTag(nbtKey);
            }
        }

        item.setItemMeta(nbtItem.toItem().getItemMeta());
    }
}
