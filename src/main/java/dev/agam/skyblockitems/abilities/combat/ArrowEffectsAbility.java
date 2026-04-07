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

public class ArrowEffectsAbility extends SkyBlockAbility {

    private final String type; // FIRE_ARROW, SLOWNESS_ARROW

    public ArrowEffectsAbility(String type, String displayName) {
        super(type, displayName, TriggerType.ON_HIT, 0.0, 0.0, 1.0, 1.0);
        this.type = type;
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double duration,
            double amplifier) {
        if (!(event instanceof EntityDamageByEntityEvent))
            return false;
        EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;

        // Ensure it was a projectile hit
        if (!(e.getDamager() instanceof org.bukkit.entity.Projectile))
            return false;
        if (!(e.getEntity() instanceof LivingEntity))
            return false;

        LivingEntity target = (LivingEntity) e.getEntity();
        if (dev.agam.skyblockitems.utils.TargetUtils.isNPC(target))
            return false;

        int ticks = (int) (duration * 20);
        if (ticks <= 0)
            ticks = 100;
        int amp = (int) amplifier;

        switch (type) {
            case "FIRE_ARROW":
                target.setFireTicks(ticks);
                break;
            case "SLOWNESS_ARROW":
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amp));
                break;
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
