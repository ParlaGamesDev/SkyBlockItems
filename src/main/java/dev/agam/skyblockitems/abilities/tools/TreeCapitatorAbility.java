package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TreeCapitatorAbility extends SkyBlockAbility {

    private static final Set<Material> LOGS = Set.of(
            Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD, Material.JUNGLE_WOOD,
            Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.MANGROVE_WOOD, Material.CHERRY_WOOD);

    public TreeCapitatorAbility() {
        super("TREE_CAPITATOR", "חוצב עצים", TriggerType.ON_BLOCK_BREAK, 0.0, 5.0, 0.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        if (!(event instanceof BlockBreakEvent))
            return false;
        Block startBlock = ((BlockBreakEvent) event).getBlock();

        if (!LOGS.contains(startBlock.getType()))
            return false;

        // BFS to collect logs
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        Map<Location, Material> blocksToBreakMap = new java.util.HashMap<>();

        queue.add(startBlock);
        visited.add(startBlock);

        int maxBlocks = (int) damage;
        if (maxBlocks <= 0)
            maxBlocks = 100; // Default fallback

        while (!queue.isEmpty() && blocksToBreakMap.size() < maxBlocks) {
            Block current = queue.poll();
            
            // If our custom TreeCapitator flag is disabled at this location, skip this block.
            // We deliberately use our own flag (sbi-tree-capitator) rather than the standard
            // WG build flag, so that admins can allow tree-capitating in protected zones (e.g. spawn)
            // simply by setting the sbi-tree-capitator flag to ALLOW.
            if (!dev.agam.skyblockitems.integration.WorldGuardHook.isTreeCapitatorEnabled(player, current.getLocation())) {
                continue;
            }

            blocksToBreakMap.put(current.getLocation(), current.getType());

            // Check neighbors
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block rel = current.getRelative(x, y, z);
                        if (!visited.contains(rel) && LOGS.contains(rel.getType())) {
                            visited.add(rel);
                            queue.add(rel);
                        }
                    }
                }
            }
        }

        // Safety: if no blocks were collected (e.g. all locations blocked by WorldGuard),
        // return false so no cooldown or visual cooldown is applied.
        if (blocksToBreakMap.isEmpty()) {
            return false;
        }

        // Fire cancellable event
        ItemStack tool = player.getInventory().getItemInMainHand();
        dev.agam.skyblockitems.api.events.TreeCapitatorEvent treeEvent = new dev.agam.skyblockitems.api.events.TreeCapitatorEvent(
                player, tool, blocksToBreakMap);
        dev.agam.skyblockitems.SkyBlockItems.getInstance().getServer().getPluginManager().callEvent(treeEvent);

        if (treeEvent.isCancelled()) {
            return false;
        }

        // Perform breaking
        for (org.bukkit.Location loc : blocksToBreakMap.keySet()) {
            Block b = loc.getBlock();
            if (b.equals(startBlock)) continue; // Start block handled by the original event
            b.breakNaturally(tool);
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        List<String> lore = new ArrayList<>();
        lore.add(formatTitle("חוצב עצים", trigger));
        lore.add(COLOR_WHITE + "שובר את כל העץ במכה אחת");
        lore.add(formatManaAndCooldown(manaCost, cooldown));
        return lore;
    }
}
