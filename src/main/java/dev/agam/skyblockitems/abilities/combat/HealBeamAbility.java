package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class HealBeamAbility extends SkyBlockAbility {

    public HealBeamAbility() {
        super("HEAL_BEAM", "קרן ריפוי וקודש", TriggerType.RIGHT_CLICK, 10.0, 30.0, 5.0, 15.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        double healAmount = damage;
        double aoeRange = range;

        // Healing Logic (Self + AOE)
        heal(player, healAmount);

        for (Entity entity : player.getNearbyEntities(aoeRange, aoeRange, aoeRange)) {
            if (entity instanceof Player) {
                heal((Player) entity, healAmount);
            }
        }

        // Particle Effects
        // Hearts around healer
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.1);

        // Red "Sparks" lightning-like at player location
        for (int i = 0; i < 10; i++) {
            Location loc = player.getLocation().add((Math.random() - 0.5) * 1.5, Math.random() * 2,
                    (Math.random() - 0.5) * 1.5);
            player.getWorld().spawnParticle(Particle.DUST, loc, 3, new Particle.DustOptions(Color.RED, 1.5f));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);

        // Visual Beam (Just for aesthetics now)
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();
        for (double i = 0; i < aoeRange; i += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.DUST, point, 1, new Particle.DustOptions(Color.YELLOW, 0.8f));
        }

        return true;
    }

    private void heal(Player target, double amount) {
        double maxHp = target.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHp = Math.min(maxHp, target.getHealth() + amount);
        target.setHealth(newHp);
        target.getWorld().spawnParticle(Particle.FALLING_HONEY, target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3,
                0.05);
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        List<String> lore = new ArrayList<>();
        lore.add(formatTitle(getDisplayName(), trigger));
        lore.add(COLOR_WHITE + "מחדש " + COLOR_GREEN + (int) damage + " HP " + COLOR_WHITE + "לך ולסובבים אותך.");
        lore.add(COLOR_WHITE + "טווח ריפוי: " + COLOR_CYAN + (int) range + " בלוקים.");
        lore.add(formatManaAndCooldown(manaCost, cooldown));
        return lore;
    }
}
