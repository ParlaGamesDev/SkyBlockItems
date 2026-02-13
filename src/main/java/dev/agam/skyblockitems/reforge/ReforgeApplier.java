package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.rarity.Rarity;
import dev.agam.skyblockitems.rarity.RarityManager;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.stat.data.AbilityListData;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import io.lumine.mythic.lib.skill.handler.SkillHandler;
import io.lumine.mythic.lib.skill.trigger.TriggerType;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles the application and removal of reforges from items.
 * Uses the MMOItems data-driven API: LiveMMOItem + StatData + build().
 * Implements strict NBT tracking to ensuring only reforge-specific data is
 * wiped.
 */
public class ReforgeApplier {

    private static final String NBT_REFORGE_KEY = "skyblock.reforge";
    private static final String NBT_ADDED_STATS = "skyblock.reforge.stats_map";
    private static final String NBT_ADDED_ABILITIES = "skyblock_reforge_abilities"; // Using internal tag to track
                                                                                    // reforge-added ones
    private static final String NBT_ORIGINAL_NAME_KEY = "skyblock.original_name";
    private static final Pattern COLOR_CODE_PATTERN = Pattern
            .compile("(&[0-9a-fk-or]|&#[0-9a-fA-F]{6}|<#[0-9a-fA-F]{6}>|<gradient:[^>]+>)");

    private final SkyBlockItems plugin;
    private final StatApplier statApplier;

    public ReforgeApplier(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.statApplier = new StatApplier(plugin);
    }

    /**
     * Applies a reforge to an item using the MMOItems data-driven API.
     */
    public boolean applyReforge(ItemStack item, Reforge reforge, String itemType) {
        if (item == null || reforge == null || !item.hasItemMeta()) {
            return false;
        }

        boolean isMMO = dev.agam.skyblockitems.integration.MMOItemsStatIntegration.isMMOItem(item);

        // 1. Prepare NBT Data (Original Name)
        NBTItem preNbt = NBTItem.get(item);
        String originalName = "";
        if (preNbt.hasTag(NBT_ORIGINAL_NAME_KEY)) {
            originalName = preNbt.getString(NBT_ORIGINAL_NAME_KEY);
        } else {
            ItemMeta meta = item.getItemMeta();
            originalName = meta.hasDisplayName() ? meta.getDisplayName() : "";
            preNbt.addTag(new ItemTag(NBT_ORIGINAL_NAME_KEY, originalName));
            item.setItemMeta(preNbt.toItem().getItemMeta());
        }

        // 2. Apply the New Reforge (Master Sequence inside)
        if (isMMO && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            applyReforgeMMOItems(item, reforge, originalName);
        } else {
            // Legacy/Non-MMO path
            if (hasReforge(item)) {
                removeReforgeLegacy(item, null);
            }
            applyReforgeLegacy(item, reforge, itemType);
            upgradeRarity(item, reforge);
            applyEnchantmentsLegacy(item, reforge);
        }

        return true;
    }

    /**
     * Data-Driven approach: Uses LiveMMOItem to inject stats, then rebuilds.
     * STRICTER MERGE LOGIC implemented.
     */
    private void applyReforgeMMOItems(ItemStack item, Reforge reforge, String savedOriginalName) {
        try {
            LiveMMOItem mmoItem = new LiveMMOItem(item);
            NBTItem nbt = NBTItem.get(item);

            // 1. SUBTRACT (The Purge)
            // Use skyblock.reforge.stats_map to remembered what was added last and subtract
            // from the base item
            if (nbt.hasTag(NBT_ADDED_STATS)) {
                Map<String, Double> receipt = deserializeStats(nbt.getString(NBT_ADDED_STATS));
                statApplier.removeStatsFromReceipt(mmoItem, receipt);
            }

            // Clean up previous reforge abilities from AbilityListData (if any were added
            // natively)
            if (nbt.hasTag(NBT_ADDED_ABILITIES)) {
                Set<String> prevAbilities = new HashSet<>(Arrays.asList(nbt.getString(NBT_ADDED_ABILITIES).split(",")));
                if (mmoItem.hasData(ItemStats.ABILITIES)) {
                    AbilityListData abilityList = (AbilityListData) mmoItem.getData(ItemStats.ABILITIES);
                    abilityList.getAbilities()
                            .removeIf(ad -> prevAbilities.contains(ad.getHandler().getId().toUpperCase()));
                    mmoItem.setData(ItemStats.ABILITIES, abilityList);
                }
            }

            // Apply NEW stats to LiveMMOItem
            Map<String, Double> addedStats = new HashMap<>();
            for (Map.Entry<String, Double> entry : reforge.getStats().entrySet()) {
                String cleanId = entry.getKey().replace("mmoitems_", "");
                String mmoStatId = cleanId.toUpperCase().replace("-", "_");
                ItemStat<?, ?> stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(mmoStatId);
                if (stat == null) {
                    stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(cleanId.toUpperCase().replace("_", "-"));
                }
                if (stat != null && stat instanceof net.Indyuce.mmoitems.stat.type.DoubleStat) {
                    double current = mmoItem.hasData(stat) ? ((DoubleData) mmoItem.getData(stat)).getValue() : 0;
                    mmoItem.setData((ItemStat) stat, new DoubleData(current + entry.getValue()));
                    addedStats.put(stat.getId(), entry.getValue());
                }
            }

            // Functional Abilities
            for (String abilityId : reforge.getAbilities()) {
                String cleanId = abilityId.toUpperCase();
                SkillHandler<?> handler = io.lumine.mythic.lib.MythicLib.plugin.getSkills().getHandler(cleanId);
                if (handler != null) {
                    AbilityListData list = mmoItem.hasData(ItemStats.ABILITIES)
                            ? (AbilityListData) mmoItem.getData(ItemStats.ABILITIES)
                            : new AbilityListData();
                    list.add(new AbilityData(handler, TriggerType.RIGHT_CLICK));
                    mmoItem.setData(ItemStats.ABILITIES, list);
                }
            }

            // 2. BUILD (The Pivot point)
            // Call build() FIRST. MMOItems generates its own lore here.
            ItemStack builtItem = mmoItem.newBuilder().build();
            ItemMeta meta = builtItem.getItemMeta();
            if (meta == null)
                return;

            // Get a mutable list of the build-generated lore
            List<String> modifiedLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            // 3. INJECT (The Injections)
            // a) Restore name: [Reforge] [OriginalColors][OriginalName]
            updateItemName(meta, reforge, savedOriginalName);

            // b) Manual (+X) annotations injection into modifiedLore
            applyManualBonusAnnotations(modifiedLore, reforge);

            // c) Manual Ability injection (Name & Description from abilities.yml)
            injectManualAbilityLore(modifiedLore, reforge);

            // 4. FINAL GUARD
            // The absolute final step for lore and meta
            meta.setLore(modifiedLore);
            builtItem.setItemMeta(meta);

            // 5. NBT STAMPING (Post-injection)
            NBTItem finalNbt = NBTItem.get(builtItem);
            finalNbt.addTag(new ItemTag(NBT_REFORGE_KEY, reforge.getId()));
            finalNbt.addTag(new ItemTag(NBT_ADDED_STATS, serializeStats(addedStats)));
            finalNbt.addTag(new ItemTag(NBT_ADDED_ABILITIES, String.join(",", reforge.getAbilities())));
            finalNbt.addTag(new ItemTag(NBT_ORIGINAL_NAME_KEY, savedOriginalName));

            // Final Sync & Rarity/Enchants
            ItemStack finalItem = finalNbt.toItem();
            ItemMeta finalMeta = finalItem.getItemMeta();

            // Rarity Upgrades
            String upgrade = reforge.getRarityUpgrade();
            if (upgrade != null && !upgrade.equalsIgnoreCase("NONE")) {
                Rarity r = plugin.getRarityManager().getRarity(upgrade);
                if (r != null) {
                    plugin.getRarityManager().saveMapping(finalItem, upgrade, false);
                    finalItem = plugin.getRarityManager().applyRarity(finalItem, r, true);
                    finalMeta = finalItem.getItemMeta();
                }
            }

            // Enchantments
            for (String s : reforge.getEnchants()) {
                try {
                    String[] split = s.split(":");
                    Enchantment e = Enchantment.getByName(split[0].toUpperCase());
                    if (e == null)
                        e = Enchantment.getByKey(NamespacedKey.minecraft(split[0].toLowerCase()));
                    if (e != null)
                        finalMeta.addEnchant(e, Integer.parseInt(split[1]), true);
                } catch (Exception ignored) {
                }
            }
            finalItem.setItemMeta(finalMeta);

            // UPDATE ORIGINAL ITEM
            item.setType(finalItem.getType());
            item.setItemMeta(finalItem.getItemMeta());

            plugin.getLogger()
                    .info("[SkyBlock Reforge] Applied " + reforge.getId() + " - Manual Injection Master Sequence.");

        } catch (Throwable e) {
            plugin.getLogger().severe("[SkyBlock Reforge] Master Sequence Fail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetMMOItem(ItemStack item) {
        try {
            NBTItem nbt = NBTItem.get(item);
            if (!nbt.hasTag(NBT_ADDED_STATS) && !nbt.hasTag(NBT_ADDED_ABILITIES))
                return;

            LiveMMOItem mmoItem = new LiveMMOItem(item);

            // 1. Remove Tracked Stats (Safe Subtraction)
            if (nbt.hasTag(NBT_ADDED_STATS)) {
                Map<String, Double> trackedStats = deserializeStats(nbt.getString(NBT_ADDED_STATS));
                for (Map.Entry<String, Double> entry : trackedStats.entrySet()) {
                    String statId = entry.getKey();
                    double addedValue = entry.getValue();

                    ItemStat stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(statId);
                    if (stat != null && mmoItem.hasData(stat)) {
                        if (stat instanceof net.Indyuce.mmoitems.stat.type.DoubleStat) {
                            DoubleData current = (DoubleData) mmoItem.getData(stat);
                            // Prevent negative or zero if it was exactly that (floating point safe)
                            double newValue = current.getValue() - addedValue;

                            if (newValue <= 0.0001) {
                                mmoItem.removeData(stat);
                            } else {
                                // Compatibility: Ensure mmoitem.setData is used correctly for DoubleStat types
                                mmoItem.setData(stat, new DoubleData(newValue));
                            }
                        }
                    }
                }
            }

            // 2. Remove Tracked Abilities
            if (nbt.hasTag(NBT_ADDED_ABILITIES) && mmoItem.hasData(ItemStats.ABILITIES)) {
                String abilitiesStr = nbt.getString(NBT_ADDED_ABILITIES);
                if (!abilitiesStr.isEmpty()) {
                    Set<String> toRemove = Arrays.stream(abilitiesStr.split(",")).collect(Collectors.toSet());
                    AbilityListData abilityList = (AbilityListData) mmoItem.getData(ItemStats.ABILITIES);

                    // We must filter cleanly
                    AbilityListData cleanedList = new AbilityListData();
                    for (AbilityData ad : abilityList.getAbilities()) {
                        if (!toRemove.contains(ad.getHandler().getId().toUpperCase())) {
                            cleanedList.add(ad);
                        }
                    }

                    if (cleanedList.getAbilities().isEmpty()) {
                        mmoItem.removeData(ItemStats.ABILITIES);
                    } else {
                        mmoItem.setData(ItemStats.ABILITIES, cleanedList);
                    }
                }
            }

            // 3. Rebuild to clean lore/NBT
            ItemStack cleaned = mmoItem.newBuilder().build();

            // 4. Clean Reforge Tags
            NBTItem finalNbt = NBTItem.get(cleaned);
            finalNbt.removeTag(NBT_REFORGE_KEY);
            finalNbt.removeTag(NBT_ADDED_STATS);
            finalNbt.removeTag(NBT_ADDED_ABILITIES);

            item.setType(cleaned.getType());
            item.setItemMeta(finalNbt.toItem().getItemMeta());

        } catch (Exception e) {
            plugin.getLogger().warning("[Reforge] Reset failed: " + e.getMessage());
        }
    }

    private void updateItemName(ItemMeta meta, Reforge reforge, String originalName) {
        if (originalName == null || originalName.isEmpty()) {
            meta.setDisplayName(ColorUtils.colorize(reforge.getDisplayName()));
            return;
        }
        // Extract leading color codes (e.g. &a&l)
        StringBuilder sb = new StringBuilder();
        // Simple regex to grab all color/format codes at start
        Matcher matcher = COLOR_CODE_PATTERN.matcher(originalName);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() == lastEnd) {
                sb.append(matcher.group());
                lastEnd = matcher.end();
            } else
                break;
        }
        // If no color found, default to &f (White) per request or keep it raw?
        // User said: "Recover the original name's color codes."
        String colors = sb.toString();
        if (colors.isEmpty())
            colors = "&f"; // Ensure not white-washed by Reforge Prefix

        String cleanName = originalName.substring(lastEnd);
        // [Reforge] [OriginalColors][OriginalName]
        String finalName = reforge.getDisplayName() + " " + colors + cleanName;
        meta.setDisplayName(ColorUtils.colorize(finalName));
    }

    private void applyManualBonusAnnotations(List<String> lore, Reforge reforge) {
        // Map of display name components -> (StatID, bonus value)
        Map<String, Map.Entry<String, Double>> bonuses = new HashMap<>();
        for (Map.Entry<String, Double> entry : reforge.getStats().entrySet()) {
            String statId = entry.getKey().replace("mmoitems_", "").toUpperCase().replace("-", "_");
            String displayName = getStatDisplayName(statId);
            if (displayName != null) {
                bonuses.put(ColorUtils.stripColor(displayName).trim(),
                        new AbstractMap.SimpleEntry<>(statId, entry.getValue()));
            }
        }

        // Loop through each line of lore already built by MMOItems
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String stripped = ColorUtils.stripColor(line);

            for (Map.Entry<String, Map.Entry<String, Double>> bonusEntry : bonuses.entrySet()) {
                String statName = bonusEntry.getKey();

                // if (line.contains(statDisplayName))
                if (stripped.contains(statName)) {
                    // Prevent double annotation
                    if (stripped.contains("(+"))
                        continue;

                    String statId = bonusEntry.getValue().getKey();
                    double val = bonusEntry.getValue().getValue();
                    String strVal = (val % 1 == 0) ? String.valueOf((long) val) : String.valueOf(val);

                    // Pull symbol
                    String symbol = plugin.getReforgeManager().getStatSymbol(statId);
                    String color = val >= 0 ? "§a" : "§c";
                    String prefix = val >= 0 ? "+" : "";

                    // Update the line: lore.set(i, line + " §a(" + symbol + " +" + value + ")");
                    String annotation = " " + color + "(" + symbol + prefix + strVal + ")";
                    lore.set(i, line + annotation);
                    break;
                }
            }
        }
    }

    private void injectManualAbilityLore(List<String> lore, Reforge reforge) {
        List<String> abilityIds = reforge.getAbilities();
        if (abilityIds == null || abilityIds.isEmpty())
            return;

        // Gap
        if (!lore.isEmpty())
            lore.add("");

        for (String id : abilityIds) {
            SkyBlockAbility ability = plugin.getAbilityManager().getAbility(id);
            if (ability != null) {
                // Header: §6Ability: Name §e§lRIGHT CLICK
                lore.add("§6Ability: " + ability.getDisplayName() + " §e§lRIGHT CLICK");
                // Body: Description from abilities.yml
                lore.addAll(ability.getDescription());
            }
        }
    }

    private String getStatDisplayName(String statIdRaw) {
        try {
            ItemStat stat = net.Indyuce.mmoitems.MMOItems.plugin.getStats().get(statIdRaw);
            if (stat != null)
                return stat.getName();
        } catch (Exception e) {
        }
        return plugin.getReforgeManager().formatStatName(statIdRaw);
    }

    // Standard Helpers...
    public String getCurrentReforge(ItemStack item) {
        if (item == null)
            return null;
        NBTItem nbt = NBTItem.get(item);
        return nbt.hasTag(NBT_REFORGE_KEY) ? nbt.getString(NBT_REFORGE_KEY) : null;
    }

    public boolean hasReforge(ItemStack item) {
        return getCurrentReforge(item) != null;
    }

    private void upgradeRarity(ItemStack item, Reforge reforge) {
        if (plugin.getRarityManager() == null)
            return;
        Rarity target = plugin.getRarityManager().getRarity(reforge.getRarityUpgrade());
        if (target != null)
            plugin.getRarityManager().applyRarity(item, target, false);
    }

    private void applyReforgeLegacy(ItemStack item, Reforge reforge, String itemType) {
        statApplier.applyStats(item, reforge, itemType);
    }

    private void applyEnchantmentsLegacy(ItemStack item, Reforge reforge) {
        for (String s : reforge.getEnchants()) {
            try {
                String[] split = s.split(":");
                Enchantment ench = Enchantment.getByName(split[0].toUpperCase());
                if (ench == null)
                    ench = Enchantment.getByKey(NamespacedKey.minecraft(split[0].toLowerCase()));
                if (ench != null)
                    item.addUnsafeEnchantment(ench, Integer.parseInt(split[1]));
            } catch (Exception e) {
            }
        }
    }

    private void removeReforgeLegacy(ItemStack item, Reforge dummy) {
        NBTItem nbt = NBTItem.get(item);
        nbt.removeTag(NBT_REFORGE_KEY);
        if (nbt.hasTag(NBT_ORIGINAL_NAME_KEY)) {
            String orig = nbt.getString(NBT_ORIGINAL_NAME_KEY);
            ItemMeta meta = nbt.toItem().getItemMeta();
            meta.setDisplayName(orig);
            item.setItemMeta(meta);
        }
        statApplier.removeStats(item, dummy);
    }

    public void removeReforge(ItemStack item, Reforge reforge) {
        if (dev.agam.skyblockitems.integration.MMOItemsStatIntegration.isMMOItem(item)) {
            resetMMOItem(item);
        } else {
            removeReforgeLegacy(item, reforge);
        }
    }

    private String serializeStats(Map<String, Double> stats) {
        return stats.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(";"));
    }

    private Map<String, Double> deserializeStats(String data) {
        Map<String, Double> map = new HashMap<>();
        if (data == null || data.isEmpty())
            return map;
        for (String pair : data.split(";")) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                try {
                    map.put(kv[0], Double.parseDouble(kv[1]));
                } catch (Exception e) {
                }
            }
        }
        return map;
    }
}
