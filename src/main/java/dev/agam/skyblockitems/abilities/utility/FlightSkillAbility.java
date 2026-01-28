package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

public class FlightSkillAbility extends SkyBlockAbility {

    public FlightSkillAbility() {
        super("FLIGHT_SKILL", "יכולת תעופה", TriggerType.RIGHT_CLICK, 30.0, 50.0, 5.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double duration,
            double range) {
        // Skip if player is in Creative/Spectator (they already have flight)
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR)
            return false;

        // Save the original flight state BEFORE enabling
        final boolean hadAllowFlight = player.getAllowFlight();
        final boolean wasFlying = player.isFlying();

        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§b✨ יכולת התעופה הופעלה למשך " + (int) duration + " שניות!");

        int ticks = (int) (duration * 20);
        Bukkit.getScheduler().runTaskLater(SkyBlockItems.getInstance(), () -> {
            if (player.isOnline() && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                    && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                player.setFlying(wasFlying);
                player.setAllowFlight(hadAllowFlight);
                player.sendMessage("§c⌚ יכולת התעופה נגמרה.");
            }
        }, ticks);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
