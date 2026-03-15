package dev.agam.skyblockitems.integration;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.event.ItemBuildEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbilityLoreListener implements Listener {

    // Regex to identify placeholders like {damage}, {mana}, etc.
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_-]+)\\}");

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemBuild(ItemBuildEvent event) {
        try {
            ItemStack itemStack = event.getItemStack();
            if (itemStack == null)
                return;

            // Check if our custom stat was applied (via NBT tag we set in the Stat)
            NBTItem nbtItem = NBTItem.get(itemStack);
            if (!nbtItem.hasTag("NATURAL_ABILITY_LORE")) {
                return;
            }

            // Now we can access the full MMOItem data safely because build is mostly done
            net.Indyuce.mmoitems.api.item.mmoitem.MMOItem mmoItem = new net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem(
                    itemStack);

            if (!mmoItem.hasData(net.Indyuce.mmoitems.ItemStats.ABILITIES)) {
                return;
            }

            net.Indyuce.mmoitems.stat.data.type.StatData statData = mmoItem
                    .getData(net.Indyuce.mmoitems.ItemStats.ABILITIES);
            if (!(statData instanceof net.Indyuce.mmoitems.stat.data.AbilityListData))
                return;

            net.Indyuce.mmoitems.stat.data.AbilityListData abilityList = (net.Indyuce.mmoitems.stat.data.AbilityListData) statData;

            // Prepare config
            org.bukkit.configuration.file.FileConfiguration config = SkyBlockItems.getInstance().getAbilitiesConfig();
            List<String> loreToAdd = new ArrayList<>();

            // Reflection-based calling of getAbilities to handle both Set and List returns
            java.util.Collection<net.Indyuce.mmoitems.stat.data.AbilityData> abilities = null;
            try {
                java.lang.reflect.Method getAbilitiesMethod = abilityList.getClass().getMethod("getAbilities");
                Object result = getAbilitiesMethod.invoke(abilityList);
                if (result instanceof java.util.Collection) {
                    abilities = (java.util.Collection<net.Indyuce.mmoitems.stat.data.AbilityData>) result;
                }
            } catch (Exception ex) {
                // Silently fail or log sparingly
            }

            if (abilities == null)
                return;

            // Iterate over ALL abilities
            for (net.Indyuce.mmoitems.stat.data.AbilityData ability : abilities) {
                String abilityId = ability.getAbility().getHandler().getId();

                // ID normalization
                String lowerId = abilityId.toLowerCase();
                String kebabId = lowerId.replace('_', '-').replace(' ', '-');
                String upperId = abilityId.toUpperCase();
                String snakeId = abilityId.replace(" ", "_").toUpperCase();

                List<String> formatProxy = null; // Declare here to be accessible in common processing

                // 1. Check CUSTOM ABILITIES (Header + Name + Desc)
                String customPath = null;
                if (config.isConfigurationSection("custom-abilities." + abilityId))
                    customPath = "custom-abilities." + abilityId;
                else if (config.isConfigurationSection("custom-abilities." + lowerId))
                    customPath = "custom-abilities." + lowerId;
                else if (config.isConfigurationSection("custom-abilities." + kebabId))
                    customPath = "custom-abilities." + kebabId;
                else if (config.isConfigurationSection("custom-abilities." + upperId))
                    customPath = "custom-abilities." + upperId;

                if (customPath != null) {
                    // It's a Custom Ability!
                    String displayName = config.getString(customPath + ".name", ability.getAbility().getName());
                    List<String> description = config.getStringList(customPath + ".description");

                    // Add Header
                    String headerFormat = config.getString("ability-header-format");
                    if (headerFormat != null && !headerFormat.isEmpty()) {
                        String triggerName = ability.getTrigger().getName();
                        String formattedHeader = headerFormat
                                .replace("{ability}", displayName)
                                .replace("{trigger}", triggerName);
                        loreToAdd.add(ChatColor.translateAlternateColorCodes('&', formattedHeader));
                    }

                    // Process Description
                    formatProxy = description; // We use the common loop below
                } else {
                    // 2. Check STANDARD ABILITIES (Desc ONLY, Back to "Previous Behavior")
                    if (config.contains("abilities." + abilityId))
                        formatProxy = config.getStringList("abilities." + abilityId);
                    else if (config.contains("abilities." + lowerId))
                        formatProxy = config.getStringList("abilities." + lowerId);
                    else if (config.contains("abilities." + kebabId))
                        formatProxy = config.getStringList("abilities." + kebabId);
                    else if (config.contains("abilities." + upperId))
                        formatProxy = config.getStringList("abilities." + upperId);
                    else if (config.contains("abilities." + snakeId))
                        formatProxy = config.getStringList("abilities." + snakeId);

                    // Standard abilities get NO header added by us (as requested)
                }

                if (formatProxy == null || formatProxy.isEmpty())
                    continue;

                // Common Placeholders Processing (for description lines)
                for (String line : formatProxy) {
                    Matcher matcher = PLACEHOLDER_PATTERN.matcher(line);
                    StringBuffer sb = new StringBuffer();

                    while (matcher.find()) {
                        String key = matcher.group(1);

                        if (key.equalsIgnoreCase("name")) {
                            matcher.appendReplacement(sb, Matcher.quoteReplacement(ability.getAbility().getName()));
                            continue;
                        }

                        double value = ability.getModifier(key);
                        String valStr;
                        if (value == Math.floor(value) && !Double.isInfinite(value)) {
                            valStr = String.valueOf((int) value);
                        } else {
                            valStr = String.valueOf(value);
                        }

                        matcher.appendReplacement(sb, valStr);
                    }
                    matcher.appendTail(sb);

                    loreToAdd.add(ChatColor.translateAlternateColorCodes('&', sb.toString()));
                }
            }

            // Inject into the final ItemStack lore
            if (!loreToAdd.isEmpty()) {
                ItemMeta meta = itemStack.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                lore.addAll(loreToAdd);

                meta.setLore(lore);
                itemStack.setItemMeta(meta);
            }

        } catch (Throwable e) {
            // Log strictly necessary errors
            e.printStackTrace();
        }
    }
}
