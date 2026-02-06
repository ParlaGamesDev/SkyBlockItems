package dev.agam.skyblockitems.abilities.booleans;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NightVisionStat extends BooleanStat {

    public NightVisionStat() {
        super("SKYBLOCK_NIGHT_VISION_CHARM",
                Material.ENDER_EYE,
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("custom-abilities.NIGHT_VISION_CHARM.name", "קמע ראיית לילה"),
                new String[] { "§7מעניק ראיית לילה קבועה כל עוד הפריט בהוטבר.", "",
                        "This stat was created from the SkyBlockItems plugin" },
                new String[] { "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("SKYBLOCK_NIGHT_VISION_CHARM", true));

        // Build the lore from config
        FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig();
        List<String> description = abilitiesConfig.getStringList("custom-abilities.NIGHT_VISION_CHARM.description");
        String configDisplayName = abilitiesConfig.getString("custom-abilities.NIGHT_VISION_CHARM.name",
                "קמע ראיית לילה");

        String triggerName = "פסיבי";
        try {
            String configTriggerRaw = abilitiesConfig.getString("custom-abilities.NIGHT_VISION_CHARM.trigger");
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
