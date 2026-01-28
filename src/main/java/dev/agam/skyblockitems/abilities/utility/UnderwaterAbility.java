package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class UnderwaterAbility extends SkyBlockAbility {

    public UnderwaterAbility() {
        super("UNDERWATER_MASTER", "אדון המים", TriggerType.UNDERWATER, 0.0, 0.0, 1.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // amplifier = damage
        int level = (int) damage;
        if (level <= 0)
            level = 1;

        // Apply effects for a short duration while active
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 60, 0, true, false, true));

        if (player.isInWater()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 60, level - 1, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 60, 0, true, false, true));
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
