package dev.agam.skyblockitems.abilities.booleans;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InfiniteReservoirStat extends BooleanStat {

    public InfiniteReservoirStat() {
        super("SKYBLOCK_INFINITE_RESERVOIR",
                Material.WATER_BUCKET,
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("custom-abilities.INFINITE_RESERVOIR.name", "מאגר מים אינסופי"),
                new String[] { "§7דלי המכיל בתוכו מים שלא נגמרים לעולם.", "",
                        "This stat was created from the SkyBlockItems plugin" },
                new String[] { "tool", "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("SKYBLOCK_INFINITE_RESERVOIR", true));

        // Add glow effect without enchantments showing
        item.getMeta().addEnchant(Enchantment.LURE, 1, true);
        item.getMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Build the lore from config
        FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig();
        List<String> description = abilitiesConfig.getStringList("custom-abilities.INFINITE_RESERVOIR.description");
        String configDisplayName = abilitiesConfig.getString("custom-abilities.INFINITE_RESERVOIR.name",
                "מים אינסופיים");

        String triggerName = "פסיבי";
        try {
            String configTriggerRaw = abilitiesConfig.getString("custom-abilities.INFINITE_RESERVOIR.trigger");
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
