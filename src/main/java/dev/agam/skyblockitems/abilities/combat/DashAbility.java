package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class DashAbility extends SkyBlockAbility {

    public DashAbility() {
        super("DASH", "דאש מהיר", TriggerType.SHIFT_RIGHT_CLICK, 8.0, 40.0, 1.5, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // damage parameter is used as Dash Power/Multiplier
        double power = damage;
        if (power <= 0)
            power = 1.5;

        // Dash logic: Push forward in a straight line
        Vector dir = player.getLocation().getDirection();

        // Ensure we don't go too high or too low, but follow the look vector
        // To make it feel like a "dash", we give it a strong forward push
        player.setVelocity(dir.multiply(power));

        // Sound and Particles
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.5f);
        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        List<String> lore = new ArrayList<>();
        lore.add(formatTitle(getDisplayName(), trigger));
        lore.add(COLOR_WHITE + "מבצע זינוק מהיר וחד קדימה.");
        lore.add(formatManaAndCooldown(manaCost, cooldown));
        return lore;
    }
}
