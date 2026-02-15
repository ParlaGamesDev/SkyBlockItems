package dev.agam.skyblockitems.stats;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Custom MMOItems stat for item rarity.
 * When an MMOItem is generated with this stat, the rarity system syncs with it.
 */
public class RarityStat extends StringStat {

    public RarityStat() {
        super("SKYBLOCK_RARITY",
                Material.NETHER_STAR,
                "Item Rarity",
                new String[] { "§7The rarity tier of this item.", "§7Valid values: Common, Rare,",
                        "§7Epic, Legendary" },
                new String[] { "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
        String rarityId = data.toString();
        if (rarityId == null || rarityId.isEmpty()) {
            return;
        }

        // Store the rarity in NBT - the RarityListener will handle lore formatting
        item.addItemTag(new ItemTag("skyblock_rarity", rarityId));
        item.addItemTag(new ItemTag("skyblock_rarity_custom", false)); // MMOItems-based, not command-based
    }
}
