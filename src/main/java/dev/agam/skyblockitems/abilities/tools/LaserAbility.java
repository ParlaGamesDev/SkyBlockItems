package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import dev.agam.skyblockitems.integration.WorldGuardHook;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LaserAbility extends SkyBlockAbility {

    public LaserAbility() {
        super("LASER", "לייזר חציבה", TriggerType.SHIFT_RIGHT_CLICK, 2.0, 15.0, 0.0, 7.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        double maxDist = damage > 0 ? damage : 15;

        Location start = player.getEyeLocation();
        Vector eyeDir = start.getDirection().normalize();

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

        Set<Block> toBreak = new LinkedHashSet<>();
        List<Location> particlePoints = new ArrayList<>();

        for (double i = 0.5; i <= maxDist; i += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(i));
            particlePoints.add(point);

            Block block = point.getBlock();
            if (block.getType() != Material.AIR && !isUnbreakable(block.getType())) {
                if (WorldGuardHook.isAbilitiesEnabled(player, point)) {
                    toBreak.add(block);
                }
            }
        }

        if (toBreak.isEmpty()) {
            return false;
        }

        player.getWorld().playSound(start, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 1.5f);
        player.getWorld().playSound(start, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2.0f);

        for (Location point : particlePoints) {
            player.getWorld().spawnParticle(Particle.DUST, point, 2, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.RED, 0.6f));
        }

        for (Block block : toBreak) {
            Location point = block.getLocation().add(0.5, 0.5, 0.5);
            var brokenData = block.getBlockData();
            block.breakNaturally(player.getInventory().getItemInMainHand());
            player.getWorld().spawnParticle(Particle.BLOCK, point, 5, 0.1, 0.1, 0.1, 0.05, brokenData);
        }

        return true;
    }

    private boolean isUnbreakable(Material type) {
        return type == Material.BEDROCK || type == Material.BARRIER || type == Material.END_PORTAL_FRAME;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
