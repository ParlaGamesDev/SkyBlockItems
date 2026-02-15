package dev.agam.skyblockitems.utils;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class AbilityLoreGenerator {

    public static List<String> generateLore(String abilityId, String displayName, String modifierString) {
        List<String> loreLines = new ArrayList<>();
        FileConfiguration abilitiesConfig = SkyBlockItems.getInstance().getAbilitiesConfig();

        String[] params = modifierString.trim().split("\\s+");
        double cd = 0;
        int mana = 0;
        try {
            if (params.length >= 1)
                cd = Double.parseDouble(params[0]);
            if (params.length >= 2)
                mana = (int) Double.parseDouble(params[1]);
        } catch (Exception ignored) {
        }

        // 1. Resolve Config Path
        String customPath = null;
        if (abilitiesConfig.isConfigurationSection("custom-abilities." + abilityId))
            customPath = "custom-abilities." + abilityId;
        else if (abilitiesConfig.isConfigurationSection("custom-abilities." + abilityId.toLowerCase()))
            customPath = "custom-abilities." + abilityId.toLowerCase();
        else if (abilitiesConfig.isConfigurationSection("custom-abilities." + abilityId.toUpperCase()))
            customPath = "custom-abilities." + abilityId.toUpperCase();

        // 2. Resolve Trigger Name
        String triggerName = "לחיצה ימנית";
        SkyBlockAbility ability = SkyBlockItems.getInstance().getAbilityManager().getAbility(abilityId);
        if (ability != null) {
            triggerName = ability.getDefaultTrigger().getDisplayName();
        }

        if (customPath != null) {
            String configDisplayName = abilitiesConfig.getString(customPath + ".name", displayName);
            List<String> configDescription = abilitiesConfig.getStringList(customPath + ".description");

            // Resolve Trigger from Config
            String configTriggerRaw = abilitiesConfig.getString(customPath + ".trigger");
            String configTrigger = triggerName;
            if (configTriggerRaw != null) {
                try {
                    TriggerType type = TriggerType.valueOf(configTriggerRaw.toUpperCase());
                    configTrigger = type.getDisplayName();
                } catch (IllegalArgumentException e) {
                    configTrigger = configTriggerRaw;
                }
            }

            // 3. Build Header
            String headerFormat = abilitiesConfig.getString("ability-header-format");
            if (headerFormat != null && !headerFormat.isEmpty()) {
                String formattedHeader = headerFormat
                        .replace("{ability}", configDisplayName)
                        .replace("{trigger}", configTrigger);
                loreLines.add(ColorUtils.translate(formattedHeader));
            } else {
                String header = SkyBlockItems.getInstance().getConfigManager().getMessage("ability.format")
                        .replace("{ability}", configDisplayName)
                        .replace("{trigger}", configTrigger);
                loreLines.add(ColorUtils.translate(header));
            }

            // 4. Process Description
            for (String line : configDescription) {
                String processed = line;

                if (params.length >= 1) {
                    if (cd <= 0 && processed.contains("{cooldown}")) {
                        processed = processed.replaceAll(",?\\s*.*זמן המתנה של.*\\{cooldown\\}.*שניות\\.?", ".");
                        processed = processed.replace("{cooldown}", "0");
                    } else {
                        processed = processed.replace("{cooldown}", params[0]);
                    }
                }
                if (params.length >= 2) {
                    if (mana <= 1.0 && processed.contains("{mana}")) {
                        processed = processed.replaceAll("בעזרת.*\\{mana\\}.*מאנה,?\\s*", "");
                        processed = processed.replace("{mana}", "0");
                    } else {
                        processed = processed.replace("{mana}", params[1]);
                    }
                }
                if (params.length >= 3) {
                    processed = processed.replace("{damage}", params[2]).replace("{radius}", params[2])
                            .replace("{duration}", params[2]).replace("{range}", params[2])
                            .replace("{value}", params[2]);
                }
                if (params.length >= 4) {
                    processed = processed.replace("{power}", params[3]).replace("{amplifier}", params[3]);
                }
                if (params.length >= 5) {
                    processed = processed.replace("{extra}", params[4]);
                }

                // Solar Repair Logic
                String normalizedId = abilityId.replace("-", "_").toUpperCase();
                if (normalizedId.equals("SOLAR_REPAIR")) {
                    double configAmount = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.charge-amount", 1.0);
                    double configMax = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.max-charge", 120.0);
                    processed = processed.replace("{damage}", String.valueOf(configAmount));
                    processed = processed.replace("{range}", String.valueOf((int) configMax));
                    processed = processed.replace("{charge}", "0");
                }

                String processedTranslated = ColorUtils.translate(processed);
                String stripped = ChatColor.stripColor(processedTranslated).trim();

                if (!stripped.isEmpty() && !stripped.equals(".")) {
                    loreLines.add(processedTranslated);
                }
            }
        } else {
            // Fallback
            String header = SkyBlockItems.getInstance().getConfigManager().getMessage("ability.format")
                    .replace("{ability}", displayName)
                    .replace("{trigger}", triggerName);
            loreLines.add(ColorUtils.translate(header));
            loreLines.add(SkyBlockItems.getInstance().getConfigManager().getMessage("ability.missing-config")
                    .replace("{id}", abilityId));
        }

        return loreLines;
    }
}
