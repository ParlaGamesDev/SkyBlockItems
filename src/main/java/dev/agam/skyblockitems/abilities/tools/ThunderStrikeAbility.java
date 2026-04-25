package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import dev.agam.skyblockitems.api.events.AbilityBlockBreakEvent;
import dev.agam.skyblockitems.integration.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ThunderStrikeAbility extends SkyBlockAbility {

    public ThunderStrikeAbility() {
        super("THUNDER_STRIKE", "מכת ברק (כריתה)", TriggerType.RIGHT_CLICK, 10.0, 40.0, 0.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        if (!(event instanceof PlayerInteractEvent))
            return false;
        PlayerInteractEvent e = (PlayerInteractEvent) event;

        Block startBlock = e.getClickedBlock();
        if (startBlock == null || !isLog(startBlock.getType()))
            return false;

        if (!WorldGuardHook.isTreeMassBreakAllowed(player, startBlock.getLocation())) {
            return false;
        }

        Set<Block> treeBlocks = new HashSet<>();
        findTreeBlocks(startBlock, treeBlocks, 0);

        ItemStack tool = player.getInventory().getItemInMainHand();
        Map<Location, Material> toBreak = new HashMap<>();
        for (Block b : treeBlocks) {
            if (WorldGuardHook.isTreeMassBreakAllowed(player, b.getLocation())) {
                toBreak.put(b.getLocation().clone(), b.getType());
            }
        }
        if (toBreak.isEmpty()) {
            return false;
        }

        AbilityBlockBreakEvent abilityEvent = new AbilityBlockBreakEvent(player, this, tool, toBreak);
        Bukkit.getPluginManager().callEvent(abilityEvent);
        if (abilityEvent.isCancelled()) {
            return false;
        }

        startBlock.getWorld().strikeLightningEffect(startBlock.getLocation());

        for (Map.Entry<Location, Material> entry : toBreak.entrySet()) {
            Block b = entry.getKey().getBlock();
            if (b.getType() == entry.getValue()) {
                b.breakNaturally(tool);
            }
        }

        return true;
    }

    private void findTreeBlocks(Block block, Set<Block> found, int depth) {
        if (depth > 500 || found.size() > 500 || !isLog(block.getType()))
            return;
        if (!found.add(block))
            return;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0)
                        continue;
                    findTreeBlocks(block.getRelative(x, y, z), found, depth + 1);
                }
            }
        }
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.contains("LOG") || name.contains("WOOD") || name.contains("STEM");
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
