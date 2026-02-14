package dev.agam.skyblockitems.abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import java.util.List;
import java.util.ArrayList;

public class GenericAbility extends SkyBlockAbility {

    public GenericAbility(String id, String name) {
        // Use default values for everything since this is just a placeholder for lore
        // identification
        super(id, name, TriggerType.RIGHT_CLICK, 0, 0, 0, 0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // Generic abilities have no Java implementation.
        return false;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        // Lore is handled by the configuration or manually preserved.
        return new ArrayList<>();
    }
}
