package dev.agam.skyblockitems.integration;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.booleans.InfiniteReservoirStat;
import dev.agam.skyblockitems.abilities.booleans.NightVisionStat;
import dev.agam.skyblockitems.abilities.booleans.SolarStat;
import dev.agam.skyblockitems.abilities.booleans.UnderwaterStat;
import dev.agam.skyblockitems.stats.DisableAnvilStat;
import dev.agam.skyblockitems.stats.DisableEnchantingStat;
import dev.agam.skyblockitems.stats.GlowStat;
import dev.agam.skyblockitems.stats.NaturalAbilityLoreStat;
import net.Indyuce.mmoitems.MMOItems;

public class MMOItemsHook {

    public MMOItemsHook() {
    }

    public void registerStats() {
        // Dynamically register all loaded abilities as MMOItems stats
        int count = 0;
        for (SkyBlockAbility ability : SkyBlockItems.getInstance().getAbilityManager().getAbilities().values()) {
            String normalizedId = ability.getId().toUpperCase().replace("-", "_");

            // Skip abilities registered as custom stats
            if (normalizedId.equals("SOLAR_REPAIR") || normalizedId.equals("UNDERWATER_MASTER")
                    || normalizedId.equals("INFINITE_RESERVOIR") || normalizedId.equals("NIGHT_VISION_CHARM"))
                continue;

            MMOItems.plugin.getStats().register(new AbilityStat(normalizedId, ability.getDisplayName()));
            count++;
        }

        SkyBlockItems.getInstance().getLogger().info("Registered " + count + " ability stats with MMOItems!");

        MMOItems.plugin.getStats().register(new GlowStat());
        MMOItems.plugin.getStats().register(new SolarStat());
        MMOItems.plugin.getStats().register(new UnderwaterStat());
        MMOItems.plugin.getStats().register(new InfiniteReservoirStat());
        MMOItems.plugin.getStats().register(new NightVisionStat());
        MMOItems.plugin.getStats().register(new NaturalAbilityLoreStat());
        MMOItems.plugin.getStats().register(new DisableEnchantingStat());
        MMOItems.plugin.getStats().register(new DisableAnvilStat());
    }
}
