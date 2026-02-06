package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class CleanseAbility extends SkyBlockAbility {

    public CleanseAbility() {
        super("CLEANSE", "טיהור", TriggerType.RIGHT_CLICK, 30.0, 40.0, 0.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        boolean removed = false;

        // Load list of effects to cleanse from config
        List<String> effectsToRemove = SkyBlockItems.getInstance().getAbilitiesConfig()
                .getStringList("custom-abilities.CLEANSE.effects-to-remove");

        // If config list is empty, use default hardcoded list
        if (effectsToRemove.isEmpty()) {
            effectsToRemove = java.util.Arrays.asList(
                    "POISON", "WITHER", "SLOWNESS", "WEAKNESS", "BLINDNESS",
                    "NAUSEA", "HUNGER", "MINING_FATIGUE", "LEVITATION", "DARKNESS");
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            String effectName = effect.getType().getKey().getKey().toUpperCase();

            // Check if this effect is in our list (case-insensitive)
            boolean shouldRemove = false;
            for (String configEffect : effectsToRemove) {
                if (configEffect.equalsIgnoreCase(effectName) ||
                        configEffect.equalsIgnoreCase(effect.getType().getName())) {
                    shouldRemove = true;
                    break;
                }
            }

            if (shouldRemove) {
                player.removePotionEffect(effect.getType());
                removed = true;
            }
        }

        if (removed) {
            String msg = SkyBlockItems.getInstance().getConfigManager().getMessage("players.cleanse-success");
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
            player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20,
                    0.5, 0.5, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 1.5f);
        } else {
            String msg = SkyBlockItems.getInstance().getConfigManager().getMessage("players.cleanse-fail");
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
        }

        return removed;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
