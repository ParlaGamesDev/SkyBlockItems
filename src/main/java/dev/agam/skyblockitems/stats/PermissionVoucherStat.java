package dev.agam.skyblockitems.stats;

import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MMOItems string stat: one permission per stat entry; add the stat multiple times in
 * MMOItems for multiple permissions. Value format: {@code permission,chat message}
 * (first comma only — message may contain more commas). Right-click consumes the item
 * and grants the permission via Vault (see {@link dev.agam.skyblockitems.listeners.PermissionVoucherListener}).
 */
public class PermissionVoucherStat extends StringStat {

    public static final String NBT_KEY = "SKYBLOCK_PERM_VOUCHER";

    public PermissionVoucherStat() {
        super("SKYBLOCK_PERM_VOUCHER",
                Material.NAME_TAG,
                "הרשאות חד פעמיות",
                new String[] {
                        "§7פורמט: §fהרשאה,הודעה לצ'אט",
                        "§7הפסיק §fהראשון§7 מפריד בין ההרשאה לבין ההודעה (אפשר פסיקים בהמשך ההודעה).",
                        "§7לחיצה ימנית: מעניקה הרשאה ומוחקת פריט",
                        "",
                        "This stat was created by the SkyBlockItems plugin."
                },
                new String[] { "consumable", "miscellaneous", "tool", "weapon", "armor", "accessory", "all" });
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
        String value = data.toString().trim();
        if (value.isEmpty()) {
            return;
        }
        item.addItemTag(new ItemTag(NBT_KEY, value));
    }

    /**
     * @return raw stat value from NBT ({@code perm,message}), or null if absent
     */
    @Nullable
    public static String getRawPermissionString(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        NBTItem nbt = NBTItem.get(stack);
        if (!nbt.hasTag(NBT_KEY)) {
            return null;
        }
        String s = nbt.getString(NBT_KEY);
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
