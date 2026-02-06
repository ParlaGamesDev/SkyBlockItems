package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

public class DogWhistleAbility extends SkyBlockAbility {

    public DogWhistleAbility() {
        super("DOG_WHISTLE", "משרוקית חיות", TriggerType.RIGHT_CLICK, 30.0, 50.0, 30.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // range = teleport radius (using damage as radius)
        double radius = damage > 0 ? damage : 50.0;
        boolean teleported = false;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 2.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 0.5f, 1.5f);

        for (Entity e : player.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Tameable) {
                Tameable pet = (Tameable) e;
                if (pet.isTamed() && pet.getOwner() != null
                        && player.getUniqueId().equals(pet.getOwner().getUniqueId())) {
                    e.teleport(player.getLocation());
                    // Add metadata to negate next fall damage
                    e.setMetadata("NEGATE_FALL_DAMAGE",
                            new org.bukkit.metadata.FixedMetadataValue(
                                    dev.agam.skyblockitems.SkyBlockItems.getInstance(), true));

                    if (e instanceof org.bukkit.entity.LivingEntity) {
                        ((org.bukkit.entity.LivingEntity) e).setFallDistance(0.0f);
                    }
                    e.getWorld().spawnParticle(Particle.CLOUD, e.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2,
                            0.05);
                    teleported = true;
                }
            }
        }

        if (teleported) {
            player.getWorld().spawnParticle(Particle.FALLING_HONEY, player.getLocation().add(0, 1.5, 0), 10, 0.5, 0.5,
                    0.5, 0.1);
            String msg = dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfigManager()
                    .getMessage("players.dog-whistle-success");
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
        } else {
            String msg = dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfigManager()
                    .getMessage("players.dog-whistle-fail");
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
        }

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
