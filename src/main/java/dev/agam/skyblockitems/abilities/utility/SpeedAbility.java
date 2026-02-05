package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Speed ability - gives Speed 4 effect on right-click.
 * Parameters: cooldown, mana, duration (seconds), amplifier (default 3 = Speed
 * 4)
 */
public class SpeedAbility extends SkyBlockAbility {

    public SpeedAbility() {
        // Default: 10s cooldown, 30 mana, 5s duration, Speed level stored in range
        // param
        super("SPEED", "מהירות", TriggerType.RIGHT_CLICK, 10.0, 30.0, 5.0, 3.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double duration,
            double amplifier) {
        // Duration is in seconds, need to convert to ticks (20 ticks = 1 second)
        int durationTicks = (int) (duration * 20);

        // Amplifier: 0 = Speed 1, 1 = Speed 2, 2 = Speed 3, 3 = Speed 4
        int amp = (int) amplifier;
        if (amp < 0)
            amp = 3; // Default to Speed 4

        // Apply Speed effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, amp, true, true, true));

        // Visual and sound effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        // Send activation message
        String msg = dev.agam.skyblockitems.SkyBlockItems.getInstance().getMessagesConfig()
                .getString("players.speed-activated", "&b⚡ קיבלת מהירות גבוהה למשך {duration} שניות!");
        msg = msg.replace("{duration}", String.valueOf((int) duration));
        dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        // Lore is handled by AbilityStat.java from abilities.yml
        return new ArrayList<>();
    }
}
