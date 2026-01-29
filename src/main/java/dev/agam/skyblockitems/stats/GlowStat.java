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
                new String[] { "§7הגדרות עבור אפקט הניצוצות.", "", "This stat was created from the SkyBlockItems plugin"},
                new String[] { "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("SKYBLOCK_GLOW", true));
        item.getMeta().addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
        item.getMeta().addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
    }
}
