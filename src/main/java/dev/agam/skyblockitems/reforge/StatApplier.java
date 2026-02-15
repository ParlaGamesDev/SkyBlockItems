package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Handles intelligent stat application to items.
 * Supports MMOItems stats and native Minecraft attributes.
 * Refactored to use MMOItems' Data-Driven API (LiveMMOItem).
 */
public class StatApplier {

    private final SkyBlockItems plugin;

    // Combat stats - only for weapons
    private static final Set<String> COMBAT_STATS = Set.of(
            "mmoitems_attack_damage",
            "mmoitems_critical_strike_chance",
            "mmoitems_critical_strike_power");

    // Tool stats - only for tools
    private static final Set<String> TOOL_STATS = Set.of(
            "mmoitems_mining_speed",
            "mmoitems_block_power");

    // Combat item types
    private static final Set<String> COMBAT_TYPES = Set.of(
            "SWORD", "BOW", "CROSSBOW", "TRIDENT", "MACE", "DAGGER", "KATANA", "SCYTHE");

    // Tool item types
    private static final Set<String> TOOL_TYPES = Set.of(
            "PICKAXE", "AXE", "SHOVEL", "HOE", "FISHING_ROD", "DRILL");

    public StatApplier(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies stats from a reforge to an item.
     * Uses the Data-Driven MMOItems API when possible.
     */
    public void applyStats(ItemStack item, Reforge reforge, String itemType) {
        if (item == null || reforge == null)
            return;

        Map<String, Double> stats = reforge.getStats();
        if (stats.isEmpty())
            return;

        if (dev.agam.skyblockitems.integration.MMOItemsStatIntegration.isMMOItem(item)) {
            applyStatsToMMOItem(item, stats, itemType);
        } else {
            applyStatsManually(item, stats, itemType);
        }
    }

    /**
     * Data-Driven Approach for MMOItems.
     */
    private void applyStatsToMMOItem(ItemStack item, Map<String, Double> stats, String itemType) {
        try {
            LiveMMOItem mmoItem = new LiveMMOItem(item);
            boolean modified = false;

            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                String statKey = entry.getKey();
                double value = entry.getValue();

                if (!isStatValidForItemType(statKey, itemType))
                    continue;

                if (statKey.startsWith("mmoitems_")) {
                    String cleanId = statKey.substring("mmoitems_".length());
                    String mmoStatId = cleanId.toUpperCase().replace("-", "_");

                    ItemStat stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(mmoStatId);
                    if (stat == null) {
                        stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats()
                                .get(cleanId.toUpperCase().replace("_", "-"));
                    }

                    if (stat != null && stat instanceof net.Indyuce.mmoitems.stat.type.DoubleStat) {
                        double currentVal = 0;
                        if (mmoItem.hasData(stat)) {
                            currentVal = ((DoubleData) mmoItem.getData(stat)).getValue();
                        }
                        // Compatibility: Ensure mmoitem.setData is used correctly for DoubleStat types
                        mmoItem.setData(stat, new DoubleData(currentVal + value));
                        modified = true;
                    }
                }
            }

            if (modified) {
                ItemStack rebuilt = mmoItem.newBuilder().buildSilently();
                item.setItemMeta(rebuilt.getItemMeta());
            }
        } catch (Exception e) {
            // plugin.getLogger().warning("[StatApplier] Failed to apply data-driven stats:
            // " + e.getMessage()); // Removed debug log
            applyStatsManually(item, stats, itemType);
        }
    }

    /**
     * Legacy/Vanilla item path.
     */
    private void applyStatsManually(ItemStack item, Map<String, Double> stats, String itemType) {
        NBTItem nbtItem = NBTItem.get(item);
        boolean modified = false;

        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            String statKey = entry.getKey();
            if (!isStatValidForItemType(statKey, itemType))
                continue;

            applyMMOItemsStatManually(nbtItem, statKey, entry.getValue());
            modified = true;
        }

        if (modified) {
            item.setItemMeta(nbtItem.toItem().getItemMeta());
        }
    }

    private void applyMMOItemsStatManually(NBTItem nbtItem, String statKey, double value) {
        String cleanId = statKey.substring("mmoitems_".length()).toUpperCase().replace("-", "_");
        String nbtKey = "MMOITEMS_" + cleanId;
        double current = nbtItem.getDouble(nbtKey);
        nbtItem.addTag(new ItemTag(nbtKey, current + value));
    }

    /**
     * Removes stats from an item.
     */
    public void removeStats(ItemStack item, Reforge reforge) {
        if (item == null || reforge == null)
            return;

        if (dev.agam.skyblockitems.integration.MMOItemsStatIntegration.isMMOItem(item)) {
            removeStatsFromMMOItem(item, reforge);
        } else {
            removeStatsManually(item, reforge.getStats());
        }
    }

    private void removeStatsFromMMOItem(ItemStack item, Reforge reforge) {
        try {
            LiveMMOItem mmoItem = new LiveMMOItem(item);
            boolean modified = false;

            for (Map.Entry<String, Double> entry : reforge.getStats().entrySet()) {
                String statKey = entry.getKey();
                if (statKey.startsWith("mmoitems_")) {
                    String cleanId = statKey.substring("mmoitems_".length());
                    String mmoStatId = cleanId.toUpperCase().replace("-", "_");

                    ItemStat stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(mmoStatId);
                    if (stat == null) {
                        stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats()
                                .get(cleanId.toUpperCase().replace("_", "-"));
                    }

                    if (stat != null && mmoItem.hasData(stat)) {
                        DoubleData current = (DoubleData) mmoItem.getData(stat);
                        double newValue = current.getValue() - entry.getValue();
                        if (newValue <= 0)
                            mmoItem.removeData(stat);
                        else {
                            current.setValue(newValue);
                            mmoItem.setData(stat, current);
                        }
                        modified = true;
                    }
                }
            }

            if (modified) {
                ItemStack rebuilt = mmoItem.newBuilder().buildSilently();
                item.setItemMeta(rebuilt.getItemMeta());
            }
        } catch (Exception e) {
            removeStatsManually(item, reforge.getStats());
        }
    }

    private void removeStatsManually(ItemStack item, Map<String, Double> stats) {
        NBTItem nbtItem = NBTItem.get(item);
        boolean modified = false;

        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            String statKey = entry.getKey();
            if (statKey.startsWith("mmoitems_")) {
                String cleanId = statKey.substring("mmoitems_".length()).toUpperCase().replace("-", "_");
                String nbtKey = "MMOITEMS_" + cleanId;
                if (nbtItem.hasTag(nbtKey)) {
                    double newValue = nbtItem.getDouble(nbtKey) - entry.getValue();
                    if (newValue <= 0)
                        nbtItem.removeTag(nbtKey);
                    else
                        nbtItem.addTag(new ItemTag(nbtKey, newValue));
                    modified = true;
                }
            }
        }

        if (modified) {
            item.setItemMeta(nbtItem.toItem().getItemMeta());
        }
    }

    /**
     * Professional Subtraction: Removes stats listed in the "Receipt" map from a
     * LiveMMOItem.
     * 
     * @param mmoItem The LiveMMOItem instance
     * @param stats   The Map of <StatID, Value> to subtract
     */
    public void removeStatsFromReceipt(ItemStack item, LiveMMOItem mmoItem, Map<String, Double> stats) {
        if (item == null || mmoItem == null || stats == null || stats.isEmpty())
            return;

        // Use standard NBTItem.get(item) directly
        NBTItem nbtItem = NBTItem.get(item);
        boolean nbtModified = false;

        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            String statId = entry.getKey();
            double valueToSubtract = entry.getValue();

            // Clean Stat ID (Remove mmoitems_ prefix for lookup)
            String mmoStatId = statId;
            if (mmoStatId.toLowerCase().startsWith("mmoitems_")) {
                mmoStatId = mmoStatId.substring("mmoitems_".length()).toUpperCase().replace("-", "_");
            }

            // 1. MMOItems Data Removal
            ItemStat stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(mmoStatId);
            if (stat != null && mmoItem.hasData(stat)) {
                if (stat instanceof net.Indyuce.mmoitems.stat.type.DoubleStat) {
                    DoubleData current = (DoubleData) mmoItem.getData(stat);
                    double newValue = current.getValue() - valueToSubtract;

                    if (newValue <= 0.0001) {
                        mmoItem.removeData(stat);
                    } else {
                        mmoItem.setData(stat, new DoubleData(newValue));
                    }
                }
            }

            // 2. Manual NBT Sync removal (Fixes "persistent stats" bug)
            String cleanIdForNBT = mmoStatId.toUpperCase().replace("-", "_");
            String nbtKey = "MMOITEMS_" + cleanIdForNBT;
            if (nbtItem.hasTag(nbtKey)) {
                double currentNBTValue = nbtItem.getDouble(nbtKey);
                double newNBTValue = currentNBTValue - valueToSubtract;
                if (newNBTValue <= 0.0001) {
                    nbtItem.removeTag(nbtKey);
                } else {
                    nbtItem.addTag(new ItemTag(nbtKey, newNBTValue));
                }
                nbtModified = true;
            }
        }

        if (nbtModified) {
            item.setItemMeta(nbtItem.toItem().getItemMeta());
        }
    }

    private boolean isStatValidForItemType(String stat, String itemType) {
        if (!COMBAT_STATS.contains(stat) && !TOOL_STATS.contains(stat))
            return true;
        if (COMBAT_STATS.contains(stat))
            return COMBAT_TYPES.contains(itemType.toUpperCase());
        if (TOOL_STATS.contains(stat))
            return TOOL_TYPES.contains(itemType.toUpperCase());
        return true;
    }
}
