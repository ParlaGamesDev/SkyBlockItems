package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import dev.agam.skyblockitems.integration.WorldGuardHook;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;

public class HammerAbility extends SkyBlockAbility {

    public HammerAbility() {
        super("HAMMER", "פטיש חציבה", TriggerType.ON_BLOCK_BREAK, 0.0, 5.0, 0.0, 3.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        if (!(event instanceof BlockBreakEvent))
            return false;
        BlockBreakEvent e = (BlockBreakEvent) event;
        Block center = e.getBlock();

        int radius = (int) damage;

        // Raytrace to find the face
        BlockFace face = player.getFacing().getOppositeFace(); // Default fallback
        var result = player.rayTraceBlocks(6.0);
        if (result != null && result.getHitBlockFace() != null) {
            face = result.getHitBlockFace();
        }

        // Calculate offsets based on face
        List<Block> blocksToBreak = new ArrayList<>();

        // If looking at UP/DOWN, we break X/Z plane
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0)
                        continue; // Skip center
                    blocksToBreak.add(center.getRelative(x, 0, z));
                }
            }
        } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
            // Looking East/West, break Y/Z plane
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (y == 0 && z == 0)
                        continue;
                    blocksToBreak.add(center.getRelative(0, y, z));
                }
            }
        } else {
            // Looking North/South, break X/Y plane
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    if (x == 0 && y == 0)
                        continue;
                    blocksToBreak.add(center.getRelative(x, y, 0));
                }
            }
        }

        boolean brokeAny = false;
        for (Block b : blocksToBreak) {
            if (b.getType() != Material.BEDROCK && b.getType() != Material.AIR) {
                if (WorldGuardHook.isAbilitiesEnabled(player, b.getLocation())) {
                    b.breakNaturally(player.getInventory().getItemInMainHand());
                    brokeAny = true;
                }
            }
        }

        return brokeAny;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        List<String> lore = new ArrayList<>();
        lore.add(formatTitle("פטיש חציבה", trigger));
        lore.add(COLOR_WHITE + "חוצב שטח של " + COLOR_GOLD + "3×3 " + COLOR_WHITE + "בלוקים");
        if (manaCost > 0) {
            lore.add(COLOR_WHITE + "עבור " + formatMana(manaCost));
        }
        return lore;
    }
}
