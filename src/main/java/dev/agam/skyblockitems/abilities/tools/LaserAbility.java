package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class LaserAbility extends SkyBlockAbility {

    public LaserAbility() {
        super("LASER", "לייזר חציבה", TriggerType.SHIFT_RIGHT_CLICK, 2.0, 15.0, 0.0, 7.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // range = distance (as defined in AbilityStat MAX_DISTANCE is params[2] which
        // maps to damage here)
        // Wait, AbilityStat says: prefix + "MAX_DISTANCE" (params[2])
        // SkyBlockAbility.java maps values[2] to damage.
        double maxDist = damage > 0 ? damage : 15;

        Location start = player.getEyeLocation();
        Vector eyeDir = start.getDirection().normalize();

        // Snap direction to nearest axis (Up, Down, North, South, East, West)
        Vector direction;
        double absX = Math.abs(eyeDir.getX());
        double absY = Math.abs(eyeDir.getY());
        double absZ = Math.abs(eyeDir.getZ());

        if (absX > absY && absX > absZ) {
            direction = new Vector(eyeDir.getX() > 0 ? 1 : -1, 0, 0);
        } else if (absY > absX && absY > absZ) {
            direction = new Vector(0, eyeDir.getY() > 0 ? 1 : -1, 0);
        } else {
            direction = new Vector(0, 0, eyeDir.getZ() > 0 ? 1 : -1);
        }

        // Sound effect (Guardian beam sound)
        player.getWorld().playSound(start, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 1.5f);
        player.getWorld().playSound(start, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);

        int blocksDestroyed = 0;
        for (double i = 0.5; i <= maxDist; i += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(i));

            // Laser red beam particles
            player.getWorld().spawnParticle(Particle.DUST, point, 2, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.RED, 0.6f));

            Block block = point.getBlock();
            if (block.getType() != Material.AIR && !isUnbreakable(block.getType())) {
                block.breakNaturally(player.getInventory().getItemInMainHand());
                blocksDestroyed++;

                // Impact particles
                player.getWorld().spawnParticle(Particle.BLOCK, point, 5, 0.1, 0.1, 0.1, 0.05, block.getBlockData());
            }
        }

        return true;
    }

    private boolean isUnbreakable(Material type) {
        return type == Material.BEDROCK || type == Material.BARRIER || type == Material.END_PORTAL_FRAME;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
