package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

public class OxygenPurityAbility extends SkyBlockAbility {

    public OxygenPurityAbility() {
        super("OXYGEN_PURITY", "טוהר החמצן", TriggerType.UNDERWATER, 60.0, 20.0, 0.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // Simple infinite breath logic
        player.setRemainingAir(player.getMaximumAir());
        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
