package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class EarthquakeAbility extends SkyBlockAbility {

    public EarthquakeAbility() {
        super("EARTHQUAKE", "רעידת אדמה", TriggerType.SHIFT_RIGHT_CLICK, 10.0, 50.0, 5.0, 1.5);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // radius = damage, power = range
        double radius = damage;
        double power = range;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.8f);

        // Ground Smash Particles
        for (double r = 0.5; r <= radius; r += 0.8) {
            for (int i = 0; i < 360; i += 30) {
                double rad = Math.toRadians(i);
                double x = Math.cos(rad) * r;
                double z = Math.sin(rad) * r;

                org.bukkit.block.Block b = player.getLocation().add(x, -0.1, z).getBlock();
                Material mat = b.getType().isAir() ? Material.DIRT : b.getType();

                player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(x, 0.1, z), 5, 0.1, 0.1, 0.1,
                        0.05, mat.createBlockData());
            }
        }

        List<Entity> nearby = player.getNearbyEntities(radius, 2.0, radius);
        for (Entity e : nearby) {
            if (e instanceof LivingEntity && e != player && !dev.agam.skyblockitems.utils.TargetUtils.isNPC(e)) {
                LivingEntity target = (LivingEntity) e;
                target.damage(power * 5, player); // Basic damage scaling

                // Realistic knockback
                Vector dir = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                dir.setY(0.4);
                target.setVelocity(dir.multiply(power));
            }
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
