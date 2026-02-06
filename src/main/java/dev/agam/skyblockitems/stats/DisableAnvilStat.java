package dev.agam.skyblockitems.stats;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class DisableAnvilStat extends BooleanStat {

    public DisableAnvilStat() {
        super("DISABLE_ANVIL",
                Material.ANVIL,
                "ביטול סדן",
                new String[] { "§7מונע מהחפץ להיכנס לסדן.", "", "This stat was created from the SkyBlockItems plugin" },
                new String[] { "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
        if (!data.isEnabled())
            return;

        item.addItemTag(new ItemTag("MMOITEMS_DISABLE_ANVIL", true));
    }
}
