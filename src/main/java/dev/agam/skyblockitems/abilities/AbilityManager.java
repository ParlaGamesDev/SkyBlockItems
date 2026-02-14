package dev.agam.skyblockitems.abilities;

import dev.agam.skyblockitems.abilities.combat.SolarRepairAbility;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    private final Map<String, SkyBlockAbility> abilities = new HashMap<>();

    public Map<String, SkyBlockAbility> getAbilities() {
        return abilities;
    }

    public void registerAbilities() {
        // Combat
        registerAbility(new dev.agam.skyblockitems.abilities.combat.PoisonAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.combat.LightningArrowAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.combat.ExplosiveArrowAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.combat.EarthquakeAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.combat.SkySmashAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.combat.HealBeamAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.combat.WebSnareAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.combat.ArrowEffectsAbility("FIRE_ARROW", "חץ אש"));
        registerAbility(new dev.agam.skyblockitems.abilities.combat.ArrowEffectsAbility("SLOWNESS_ARROW", "חץ האטה"));

        // Tools
        registerAbility(new dev.agam.skyblockitems.abilities.tools.HammerAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.LaserAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.MagnetAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.TreeCapitatorAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.BoomerangAbility());
        registerAbility(new SolarRepairAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.HawkEyeAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.LuckyTreasureAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.MinersLuckAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.ThunderStrikeAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.CharcoalConverterAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.ButchersBladeAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.tools.GoldenLeafAbility());

        // Armor/Utility
        registerAbility(new dev.agam.skyblockitems.abilities.utility.DoubleJumpAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.UnderwaterAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.OxygenPurityAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.DogWhistleAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.RetaliationAbility(true)); // Freeze
        registerAbility(new dev.agam.skyblockitems.abilities.utility.RetaliationAbility(false)); // Lightning
        // FlightSkillAbility removed by user request
        registerAbility(new dev.agam.skyblockitems.abilities.utility.CropOverdriveAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.FarmersAuraAbility("FARMER_AURA_WHEAT",
                "הילת חיטה", Material.WHEAT));
        registerAbility(new dev.agam.skyblockitems.abilities.utility.FarmersAuraAbility("FARMER_AURA_BEETROOT",
                "הילת סלק", Material.BEETROOTS));
        registerAbility(new dev.agam.skyblockitems.abilities.utility.FarmersAuraAbility("FARMER_AURA_CARROT",
                "הילת גזר", Material.CARROTS));
        registerAbility(new dev.agam.skyblockitems.abilities.utility.FarmersAuraAbility("FARMER_AURA_POTATO",
                "הילת תפוח אדמה", Material.POTATOES));
        // DiamondRadarAbility removed by user request
        registerAbility(new dev.agam.skyblockitems.abilities.utility.CleanseAbility());
        // registerAbility(new
        // dev.agam.skyblockitems.abilities.combat.OverloadAbility()); // Removed by
        // user request
        registerAbility(new dev.agam.skyblockitems.abilities.utility.MetabolismAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.MetabolismAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.SpeedAbility());
        registerAbility(new dev.agam.skyblockitems.abilities.utility.GrapplingHookAbility());

        registerAbility(new dev.agam.skyblockitems.abilities.utility.GrapplingHookAbility());

        // Load Generic Abilities from config
        loadGenericAbilities();
    }

    private void loadGenericAbilities() {
        try {
            java.io.File file = new java.io.File(dev.agam.skyblockitems.SkyBlockItems.getInstance().getDataFolder(),
                    "abilities.yml");
            if (!file.exists())
                return;

            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(file);

            for (String key : config.getKeys(false)) {
                if (key.equalsIgnoreCase("ability-header-format"))
                    continue;

                // If already registered (Java Class exists), skip
                if (abilities.containsKey(key))
                    continue;

                String name = config.getString(key + ".name");
                if (name != null) {
                    dev.agam.skyblockitems.SkyBlockItems.getInstance().getLogger()
                            .info("Registering Generic Ability: " + key + " (" + name + ")");
                    registerAbility(new GenericAbility(key, name));
                }
            }
        } catch (Exception e) {
            dev.agam.skyblockitems.SkyBlockItems.getInstance().getLogger()
                    .warning("Failed to load generic abilities: " + e.getMessage());
        }
    }

    public void registerAbility(SkyBlockAbility ability) {
        abilities.put(ability.getId(), ability);
    }

    public SkyBlockAbility getAbility(String id) {
        return abilities.get(id);
    }

    public SkyBlockAbility getAbilityByName(String name) {
        for (SkyBlockAbility ability : abilities.values()) {
            if (ability.getDisplayName().equalsIgnoreCase(name) || ability.getId().equalsIgnoreCase(name)) {
                return ability;
            }
        }
        return null; // Not found
    }
}
