package dev.agam.skyblockitems.abilities.booleans;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SolarStat extends BooleanStat {

    public SolarStat() {
        super("SKYBLOCK_SOLAR_REPAIR",
                Material.SUNFLOWER,
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("custom-abilities.SOLAR_REPAIR.name", "טעינה סולארית"),
                new String[] { "§7טעינת נזק בונוס בעמידה בשמש.", "",
                        "This stat was created from the SkyBlockItems plugin" },
                new String[] { "weapon", "tool", "armor", "accessory", "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        // Add the primary ability tag
        item.addItemTag(new ItemTag("SKYBLOCK_SOLAR_REPAIR", "true"));

        // Build the lore from config
        FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig();
        List<String> description = abilitiesConfig.getStringList("custom-abilities.SOLAR_REPAIR.description");
        String configDisplayName = abilitiesConfig.getString("custom-abilities.SOLAR_REPAIR.name", "טעינה סולארית");

        // Get trigger name
        String triggerName = "עמידה בשמש";
        try {
            String configTriggerRaw = abilitiesConfig.getString("custom-abilities.SOLAR_REPAIR.trigger");
            if (configTriggerRaw != null) {
                // Check if it's an enum or custom
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

        // Description lines
        double configAmount = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.charge-amount", 1.0);
        double configMax = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.max-charge", 120.0);

        for (String line : description) {
            String processed = line
                    .replace("{damage}", String.valueOf(configAmount))
                    .replace("{range}", String.valueOf((int) configMax))
                    .replace("{charge}", "0");

            String translated = dev.agam.skyblockitems.utils.ColorUtils.translate(processed);
            if (!org.bukkit.ChatColor.stripColor(translated).trim().isEmpty()) {
                loreLines.add(translated);
            }
        }

        // Insert into lore
        item.getLore().insert("ability-description", loreLines);
    }
}
