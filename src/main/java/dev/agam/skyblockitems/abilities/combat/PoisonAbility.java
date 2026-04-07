package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class PoisonAbility extends SkyBlockAbility {

    public PoisonAbility() {
        super("POISON", "Poison Series", TriggerType.ON_HIT, 5.0, 10.0, 2.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        LivingEntity target = null;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
            if (e.getEntity() instanceof LivingEntity) {
                target = (LivingEntity) e.getEntity();
            }
        } else if (event instanceof org.bukkit.event.entity.ProjectileHitEvent) {
            org.bukkit.event.entity.ProjectileHitEvent e = (org.bukkit.event.entity.ProjectileHitEvent) event;
            if (e.getHitEntity() instanceof LivingEntity) {
                target = (LivingEntity) e.getHitEntity();
            }
        }

        if (target == null || dev.agam.skyblockitems.utils.TargetUtils.isNPC(target))
            return false;

        int durationTicks = (int) (damage * 20);
        int amplifier = (int) range;

        // Add Poison
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, durationTicks, amplifier));

        // Add Blindness if amplifier is high
        if (amplifier >= 4) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        List<String> lore = new ArrayList<>();
        lore.add(formatTitle("סדרת הרעל", trigger));
        lore.add(COLOR_WHITE + "מחיל " + COLOR_RANGE + "רעל " + (int) damage + " " + COLOR_WHITE + "על האויב");
        if (damage >= 4) {
            lore.add(COLOR_WHITE + "גם גורם " + COLOR_DURATION + "עיוורון");
        }
        lore.add(formatManaAndCooldown(manaCost, cooldown));
        return lore;
    }
}
