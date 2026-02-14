package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class OverloadAbility extends SkyBlockAbility {

    public OverloadAbility() {
        super("OVERLOAD", "עומס יתר", TriggerType.RIGHT_CLICK, 10.0, 50.0, 100.0, 5.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // Simple AoE Explosion around player
        double radius = range > 0 ? range : 5.0; // Default radius 5

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation().add(0, 1, 0), 3);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 50, radius / 2, 1, radius / 2, 0.1);

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity && e != player) {
                ((LivingEntity) e).damage(damage, player);
                e.setVelocity(e.getLocation().toVector().subtract(player.getLocation().toVector()).normalize()
                        .multiply(1.5).setY(0.5));
            }
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
