package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class NightVisionCharmAbility extends SkyBlockAbility {

    public NightVisionCharmAbility() {
        super("NIGHT_VISION_CHARM", "קמע ראיית לילה", TriggerType.HOTBAR, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 300, 0, true, false, false));
        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
