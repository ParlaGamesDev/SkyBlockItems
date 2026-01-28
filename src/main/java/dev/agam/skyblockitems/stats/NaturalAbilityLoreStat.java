package dev.agam.skyblockitems.stats;

import dev.agam.skyblockitems.SkyBlockItems;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.stat.data.AbilityListData;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic MMOItems stat that injects configurable lore for ALL abilities on
 * the item.
 */
public class NaturalAbilityLoreStat extends BooleanStat {

    public NaturalAbilityLoreStat() {
        super("NATURAL_ABILITY_LORE", Material.BOOK,
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("natural-ability-lore-name", "Natural Ability Lore"),
                new String[] { "§7הזרקת לור מותאם אישית לכל היכולות.", "",
                        "This stat was created from the SkyBlockItems plugin" },
                new String[] { "all" });
    }

    @Override
    public void whenApplied(ItemStackBuilder item, BooleanData data) {
        if (!data.isEnabled())
            return;

        // Just mark the item so the listener knows to process it later
        item.addItemTag(new io.lumine.mythic.lib.api.item.ItemTag("NATURAL_ABILITY_LORE", true));

        // Preview Logic: Try to inject lore immediately for the Editor
        try {
            if (!item.getMMOItem().hasData(ItemStats.ABILITIES))
                return;

            StatData statData = item.getMMOItem().getData(ItemStats.ABILITIES);
            if (!(statData instanceof AbilityListData))
                return;

            AbilityListData abilityList = (AbilityListData) statData;
            FileConfiguration abilitiesConfig = SkyBlockItems.getInstance().getAbilitiesConfig();
            List<String> loreToAdd = new ArrayList<>();

            // Reflection-based safe retrieval
            java.util.Collection<AbilityData> abilities = null;
            try {
                java.lang.reflect.Method getAbilitiesMethod = abilityList.getClass().getMethod("getAbilities");
                Object result = getAbilitiesMethod.invoke(abilityList);
                if (result instanceof java.util.Collection) {
                    abilities = (java.util.Collection<AbilityData>) result;
                }
            } catch (Exception ignored) {
            }

            if (abilities == null)
                return;

            // Iterate over ALL abilities
            for (AbilityData ability : abilities) {
                String abilityId = ability.getAbility().getHandler().getId();

                // ID normalization
                String lowerId = abilityId.toLowerCase();
                String kebabId = lowerId.replace('_', '-').replace(' ', '-');
                String upperId = abilityId.toUpperCase();
                String snakeId = abilityId.replace(" ", "_").toUpperCase();

                List<String> formatProxy = null;

                // 1. Check CUSTOM ABILITIES (Header + Name + Desc)
                String customPath = null;
                if (abilitiesConfig.isConfigurationSection("custom-abilities." + abilityId))
                    customPath = "custom-abilities." + abilityId;
                else if (abilitiesConfig.isConfigurationSection("custom-abilities." + lowerId))
                    customPath = "custom-abilities." + lowerId;
                else if (abilitiesConfig.isConfigurationSection("custom-abilities." + kebabId))
                    customPath = "custom-abilities." + kebabId;
                else if (abilitiesConfig.isConfigurationSection("custom-abilities." + upperId))
                    customPath = "custom-abilities." + upperId;

                if (customPath != null) {
                    // It's a Custom Ability!
                    String displayName = abilitiesConfig.getString(customPath + ".name",
                            ability.getAbility().getName());
                    List<String> description = abilitiesConfig.getStringList(customPath + ".description");

                    // Add Header
                    String headerFormat = abilitiesConfig.getString("ability-header-format");
                    if (headerFormat != null && !headerFormat.isEmpty()) {
                        String triggerName = ability.getTrigger().getName();
                        String formattedHeader = headerFormat
                                .replace("{ability}", displayName)
                                .replace("{trigger}", triggerName);
                        loreToAdd.add(ChatColor.translateAlternateColorCodes('&', formattedHeader));
                    }

                    formatProxy = description;
                } else {
                    // 2. Check STANDARD ABILITIES (Desc ONLY)
                    if (abilitiesConfig.contains("abilities." + abilityId))
                        formatProxy = abilitiesConfig.getStringList("abilities." + abilityId);
                    else if (abilitiesConfig.contains("abilities." + lowerId))
                        formatProxy = abilitiesConfig.getStringList("abilities." + lowerId);
                    else if (abilitiesConfig.contains("abilities." + kebabId))
                        formatProxy = abilitiesConfig.getStringList("abilities." + kebabId);
                    else if (abilitiesConfig.contains("abilities." + upperId))
                        formatProxy = abilitiesConfig.getStringList("abilities." + upperId);
                    else if (abilitiesConfig.contains("abilities." + snakeId))
                        formatProxy = abilitiesConfig.getStringList("abilities." + snakeId);
                }

                if (formatProxy == null || formatProxy.isEmpty())
                    continue;

                // Process lines
                for (String line : formatProxy) {
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}")
                            .matcher(line);
                    StringBuffer sb = new StringBuffer();

                    while (matcher.find()) {
                        String key = matcher.group(1);

                        if (key.equalsIgnoreCase("name")) {
                            matcher.appendReplacement(sb,
                                    java.util.regex.Matcher.quoteReplacement(ability.getAbility().getName()));
                            continue;
                        }

                        double value = ability.getModifier(key);
                        String valStr;
                        if (value == Math.floor(value) && !Double.isInfinite(value)) {
                            valStr = String.valueOf((int) value);
                        } else {
                            valStr = String.valueOf(value);
                        }

                        matcher.appendReplacement(sb, valStr);
                    }
                    matcher.appendTail(sb);

                    loreToAdd.add(ChatColor.translateAlternateColorCodes('&', sb.toString()));
                }
            }

            if (!loreToAdd.isEmpty()) {
                item.getLore().insert("ability-description", loreToAdd);
            }

        } catch (Throwable ignored) {
            // Be silent in element preview
        }
    }
}
