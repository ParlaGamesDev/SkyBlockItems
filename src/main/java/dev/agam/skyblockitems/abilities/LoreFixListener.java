package dev.agam.skyblockitems.abilities;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.utils.AbilityLoreGenerator;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.event.ItemBuildEvent;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.stat.data.AbilityListData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LoreFixListener implements Listener {

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemBuild(ItemBuildEvent event) {
        try {
            ItemStack itemStack = event.getItemStack();
            if (itemStack == null)
                return;

            // Use LiveMMOItem to access item data from the built ItemStack
            MMOItem mmoItem = new LiveMMOItem(itemStack);

            if (!mmoItem.hasData(ItemStats.ABILITIES)) {
                return;
            }

            StatData statData = mmoItem.getData(ItemStats.ABILITIES);
            if (!(statData instanceof AbilityListData)) {
                return;
            }
            AbilityListData abilityList = (AbilityListData) statData;

            // Reflection-based safe retrieval
            Collection<AbilityData> abilities = null;
            try {
                java.lang.reflect.Method getAbilitiesMethod = abilityList.getClass().getMethod("getAbilities");
                Object result = getAbilitiesMethod.invoke(abilityList);
                if (result instanceof Collection) {
                    abilities = (Collection<AbilityData>) result;
                }
            } catch (Exception ignored) {
                return;
            }

            if (abilities == null || abilities.isEmpty())
                return;

            List<String> newLoreLines = new ArrayList<>();

            for (AbilityData ability : abilities) {
                // Safe ID retrieval via handler
                String id = ability.getAbility().getHandler().getId();
                String name = ability.getAbility().getName();

                // Build modifier string (cooldown mana damage range etc)
                StringBuilder modifierStr = new StringBuilder();
                modifierStr.append(ability.getModifier("cooldown")).append(" ");
                modifierStr.append(ability.getModifier("mana")).append(" ");
                modifierStr.append(ability.getModifier("damage")).append(" ");
                modifierStr.append(ability.getModifier("range")).append(" ");

                // Generate clean lore
                List<String> lines = AbilityLoreGenerator.generateLore(id, name, modifierStr.toString());
                newLoreLines.addAll(lines);
            }

            if (newLoreLines.isEmpty())
                return;

            ItemMeta meta = itemStack.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null)
                lore = new ArrayList<>();

            // Append our beautiful lore
            lore.addAll(newLoreLines);

            meta.setLore(lore);
            itemStack.setItemMeta(meta);

        } catch (Throwable t) {
            // Fail silently if API incompatible
            SkyBlockItems.getInstance().getLogger().warning("LoreFixListener error: " + t.getMessage());
        }
    }
}
