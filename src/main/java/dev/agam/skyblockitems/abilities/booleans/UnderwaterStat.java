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

public class UnderwaterStat extends BooleanStat {

    public UnderwaterStat() {
        super("SKYBLOCK_UNDERWATER_MASTER",
                Material.HEART_OF_THE_SEA,
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("custom-abilities.UNDERWATER_MASTER.name", "אדון המים"),
                new String[] { "§7יכולת נשימה ושחייה משופרת מתחת למים.", "",
                        "This stat was created from the SkyBlockItems plugin" },
                new String[] { "armor", "accessory", "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("SKYBLOCK_UNDERWATER_MASTER", "true"));

        FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig();
        List<String> description = abilitiesConfig.getStringList("custom-abilities.UNDERWATER_MASTER.description");
        String configDisplayName = abilitiesConfig.getString("custom-abilities.UNDERWATER_MASTER.name", "אדון המים");

        String triggerName = "פסיבי";
        try {
            String configTriggerRaw = abilitiesConfig.getString("custom-abilities.UNDERWATER_MASTER.trigger");
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
