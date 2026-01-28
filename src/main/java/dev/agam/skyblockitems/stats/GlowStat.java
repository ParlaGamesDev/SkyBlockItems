package dev.agam.skyblockitems.stats;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GlowStat extends BooleanStat {

    public GlowStat() {
        super("SKYBLOCK_GLOW",
                Material.ENCHANTING_TABLE,
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("custom-abilities.GLOW.name", "הפקת אנרגיה (Glow)"),
                new String[] { "§7הגדרות עבור אפקט הניצוצות." },
                new String[] { "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("SKYBLOCK_GLOW", true));
        item.getMeta().addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
        item.getMeta().addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);

        // Build the lore from config
        FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig();
        List<String> description = abilitiesConfig.getStringList("custom-abilities.GLOW.description");
        String configDisplayName = abilitiesConfig.getString("custom-abilities.GLOW.name", "הפקת אנרגיה");

        String triggerName = "פסיבי";
        try {
            String configTriggerRaw = abilitiesConfig.getString("custom-abilities.GLOW.trigger");
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
