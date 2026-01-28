package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RetaliationAbility extends SkyBlockAbility {

    private final boolean freezeType;

    public RetaliationAbility(boolean freezeType) {
        super(freezeType ? "RETALIATION_FREEZE" : "RETALIATION_LIGHTNING",
                freezeType ? "נקמת הכפור" : "נקמת הברק",
                TriggerType.ON_HIT_TAKEN,
                5.0, 20.0, 5.0, 30.0);
        this.freezeType = freezeType;
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        if (!(event instanceof EntityDamageByEntityEvent))
            return false;
        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;

        LivingEntity attacker = null;
        if (e.getDamager() instanceof LivingEntity) {
            attacker = (LivingEntity) e.getDamager();
        } else if (e.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) e.getDamager();
            if (proj.getShooter() instanceof LivingEntity) {
                attacker = (LivingEntity) proj.getShooter();
            }
        }

        if (attacker == null)
            return false;

        // chance = range (params[3] in AbilityStat)
        double chance = range / 100.0;
        if (new Random().nextDouble() > chance)
            return false;

        if (freezeType) {
            // Freeze the attacker
            int durationTicks = (int) (damage * 20); // damage = duration in seconds (params[2])
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 255, true, false));
            attacker.addPotionEffect(
                    new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, 255, true, false));
            attacker.setFreezeTicks(durationTicks);

            attacker.getWorld().spawnParticle(Particle.SNOWFLAKE, attacker.getLocation().add(0, 1, 0), 20, 0.5, 0.5,
                    0.5, 0);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        } else {
            // Strike lightning
            attacker.getWorld().strikeLightningEffect(attacker.getLocation());
            attacker.damage(damage, player); // damage = damage Value (params[2])

            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.5f);
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
