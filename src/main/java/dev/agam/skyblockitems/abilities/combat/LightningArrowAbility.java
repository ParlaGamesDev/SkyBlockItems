package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.ArrayList;
import java.util.List;

public class LightningArrowAbility extends SkyBlockAbility {

    public LightningArrowAbility() {
        super("LIGHTNING_ARROW", "חץ ברק", TriggerType.ON_ARROW_HIT, 8.0, 25.0, 5.0, 3.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        if (!(event instanceof ProjectileHitEvent))
            return false;
        ProjectileHitEvent e = (ProjectileHitEvent) event;

        if (!(e.getEntity() instanceof Arrow))
            return false;
        Arrow arrow = (Arrow) e.getEntity();

        // Strike lightning at hit location
        if (e.getHitEntity() != null && e.getHitEntity() instanceof LivingEntity && !dev.agam.skyblockitems.utils.TargetUtils.isNPC(e.getHitEntity())) {
            LivingEntity target = (LivingEntity) e.getHitEntity();
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.damage(damage, player);

            // Chain to nearby entities
            for (org.bukkit.entity.Entity nearby : target.getNearbyEntities(range, range, range)) {
                if (nearby instanceof LivingEntity && nearby != player && !dev.agam.skyblockitems.utils.TargetUtils.isNPC(nearby)) {
                    ((LivingEntity) nearby).damage(damage * 0.5, player);
                    nearby.getWorld().strikeLightningEffect(nearby.getLocation());
                }
            }
        } else if (e.getHitBlock() != null) {
            arrow.getWorld().strikeLightningEffect(e.getHitBlock().getLocation());
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        List<String> lore = new ArrayList<>();
        lore.add(formatTitle(getDisplayName(), trigger));
        lore.add(COLOR_WHITE + "חיצים מזמנים " + COLOR_YELLOW + "ברק " + COLOR_WHITE + "בפגיעה.");
        lore.add(COLOR_WHITE + "נזק: " + COLOR_RED + (int) damage + COLOR_WHITE + " טווח שרשרת: " + COLOR_GREEN
                + (int) range);
        lore.add(formatManaAndCooldown(manaCost, cooldown));
        return lore;
    }
}
