package dev.agam.skyblockitems.stats;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class DisableEnchantingStat extends BooleanStat {

    public DisableEnchantingStat() {
        super("DISABLE_ENCHANTING",
                Material.ENCHANTING_TABLE,
                "ביטול כישוף",
                new String[] { "§7מונע מהחפץ להיכנס לשולחן הכישופים.", "",
                        "This stat was created from the SkyBlockItems plugin" },
                new String[] { "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("MMOITEMS_DISABLE_ENCHANTING", true));
    }
}
