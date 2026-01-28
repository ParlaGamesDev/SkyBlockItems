package dev.agam.skyblockitems.stats;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DiverSetBonusStat extends BooleanStat {

    public DiverSetBonusStat() {
        super("SKYBLOCK_DIVER_SET",
                Material.PRISMARINE_CRYSTALS,
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("custom-abilities.DIVER_SET.name", "בונוס סט צוללן"),
                new String[] { "§7מעניק מהירות שחייה פי 2.5 ונשימה אינסופית." },
                new String[] { "armor", "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("SKYBLOCK_DIVER_SET", true));

        // Build the lore from config
        FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig();
        List<String> description = abilitiesConfig.getStringList("custom-abilities.DIVER_SET.description");
        String configDisplayName = abilitiesConfig.getString("custom-abilities.DIVER_SET.name", "בונוס סט צוללן");

        String triggerName = "סט מלא";
        try {
            String configTriggerRaw = abilitiesConfig.getString("custom-abilities.DIVER_SET.trigger");
            if (configTriggerRaw != null) {
                try {
                    dev.agam.skyblockitems.abilities.TriggerType type = dev.agam.skyblockitems.abilities.TriggerType
                            .valueOf(configTriggerRaw.toUpperCase());
                    triggerName = type.getDisplayName();
                } catch (IllegalArgumentException e) {
                    triggerName = configTriggerRaw;
                }
            }
        } catch (Exception ignored) {
        }

        List<String> loreLines = new ArrayList<>();

        // Header
        String headerFormat = abilitiesConfig.getString("ability-header-format", "§6{ability} §7| {trigger}");
        String formattedHeader = headerFormat
                .replace("{ability}", configDisplayName)
                .replace("{trigger}", triggerName);
        loreLines.add(dev.agam.skyblockitems.utils.ColorUtils.translate(formattedHeader));

        for (String line : description) {
            String translated = dev.agam.skyblockitems.utils.ColorUtils.translate(line);
            if (!org.bukkit.ChatColor.stripColor(translated).trim().isEmpty()) {
                loreLines.add(translated);
            }
        }
        item.getLore().insert("ability-description", loreLines);
    }
}
