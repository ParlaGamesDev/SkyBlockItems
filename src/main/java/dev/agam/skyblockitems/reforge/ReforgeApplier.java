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
import java.text.Normalizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Collection;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles the application and removal of reforges from items.
 * Uses the MMOItems data-driven API: LiveMMOItem + StatData + build().
 * Implements strict NBT tracking to ensuring only reforge-specific data is
 * wiped.
 */
public class ReforgeApplier {

    private static final String NBT_REFORGE_KEY = "skyblock.reforge";
    private static final String NBT_ADDED_STATS = "skyblock.reforge.stats_map";
    private static final String NBT_ADDED_ABILITIES = "skyblock_reforge_abilities";
    private static final String NBT_ORIGINAL_NAME_KEY = "skyblock.original_name";

    // ABSOLUTE CORE FIX: PersistentDataContainer Keys
    private final NamespacedKey PDC_REFORGE_ID;
    private final NamespacedKey PDC_STATS_RECEIPT;
    private final NamespacedKey PDC_ABILITIES;
    private final NamespacedKey PDC_ENCHANTS; // New key for tracking enchants
    private final NamespacedKey PDC_ORIGINAL_NAME;

    private static final Pattern COLOR_CODE_PATTERN = Pattern
            .compile("(&[0-9a-fk-or]|&x(&[0-9a-fA-F]){6}|&#[0-9a-fA-F]{6}|<#[0-9a-fA-F]{6}>|<gradient:[^>]+>)");

    private final SkyBlockItems plugin;
    private final StatApplier statApplier;

    public ReforgeApplier(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.statApplier = new StatApplier(plugin);

        // Initialize PDC Keys
        this.PDC_REFORGE_ID = new NamespacedKey(plugin, "reforge_id");
        this.PDC_STATS_RECEIPT = new NamespacedKey(plugin, "reforge_stats");
        this.PDC_ABILITIES = new NamespacedKey(plugin, "reforge_abilities");
        this.PDC_ENCHANTS = new NamespacedKey(plugin, "reforge_enchants");
        this.PDC_ORIGINAL_NAME = new NamespacedKey(plugin, "reforge_original_name");
    }

    /**
     * Checks if an item is eligible for reforging.
     * Requirements:
     * 1. Must be a valid MMOItems item
     * 2. Must have REFORGEABLE stat set to true (or not set, defaults to true)
     * 
     * @param item The item to check
     * @return true if the item can be reforged, false otherwise
     */
    public boolean isReforgeable(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        // Use ReforgeableStat's static method which handles all checks
        return dev.agam.skyblockitems.integration.ReforgeableStat.isReforgeable(item);
    }

    public String getReforgeId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        return item.getItemMeta().getPersistentDataContainer().get(PDC_REFORGE_ID, PersistentDataType.STRING);
    }

    /**
     * Gets the reason why an item cannot be reforged.
     * 
     * @param item The item to check
     * @return Message key for the error reason, or null if item is reforgeable
     */
    public String getNotReforgeableReason(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        NBTItem nbtItem = NBTItem.get(item);

        // Check if it's an MMOItem
        if (!nbtItem.hasTag("MMOITEMS_ITEM_ID")) {
            return "reforge.not-mmoitem";
        }

        // Check if REFORGEABLE stat is explicitly false OR missing (if missing, it's
        // not reforgeable)
        if (!nbtItem.hasTag("MMOITEMS_REFORGEABLE") || !nbtItem.getBoolean("MMOITEMS_REFORGEABLE")) {
            return "reforge.not-reforgeable";
        }

        return null; // Item is reforgeable
    }

    public boolean applyReforge(ItemStack item, Reforge reforge, String itemType) {
        if (item == null || reforge == null || !item.hasItemMeta()) {
            return false;
        }

        // 1. Fire API event (Player can be null if not initiated by a player GUI)
        dev.agam.skyblockitems.api.events.SkyBlockReforgeEvent apiEvent = 
            new dev.agam.skyblockitems.api.events.SkyBlockReforgeEvent(null, item, reforge.getId());
        org.bukkit.Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) return false;
        
        // Use potentially modified reforgeId from event
        if (!apiEvent.getReforgeId().equals(reforge.getId())) {
            reforge = plugin.getReforgeManager().getReforge(apiEvent.getReforgeId());
            if (reforge == null) return false;
        }

        boolean isMMO = dev.agam.skyblockitems.integration.MMOItemsStatIntegration.isMMOItem(item);

        // 1. Prepare PDC Data (Original Name Persistence)
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String savedOriginalName;
        if (pdc.has(PDC_ORIGINAL_NAME, org.bukkit.persistence.PersistentDataType.STRING)) {
            savedOriginalName = pdc.get(PDC_ORIGINAL_NAME, org.bukkit.persistence.PersistentDataType.STRING);
        } else {
            savedOriginalName = meta.hasDisplayName() ? meta.getDisplayName() : "";
            pdc.set(PDC_ORIGINAL_NAME, org.bukkit.persistence.PersistentDataType.STRING, savedOriginalName);
            item.setItemMeta(meta);
        }

        // 2. Apply the New Reforge (Master Sequence inside)
        if (isMMO && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            String oldReforgeId = getCurrentReforge(item);
            applyReforgeMMOItems(item, reforge, savedOriginalName, oldReforgeId);
        } else {
            // Legacy/Non-MMO path
            if (hasReforge(item)) {
                removeReforgeLegacy(item, null);
            }
            // For legacy, we just use COMMON or first available
            Reforge.RarityData data = reforge.getDataFor("COMMON");
            applyReforgeLegacy(item, reforge, itemType, data);
            applyEnchantmentsLegacy(item, data);
        }

        return true;
    }

    private List<List<String>> extractAbilitiesRaw(List<String> lore) {
        List<List<String>> abilities = new ArrayList<>();
        if (lore == null || lore.isEmpty())
            return abilities;

        List<String> currentAbility = null;
        for (String line : lore) {
            String stripped = dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.stripColor(line).trim();
            boolean isFooter = stripped.isEmpty();

            // Detect Header
            boolean isHeader = false;
            String normalized = normalizeForMatch(stripped);
            if (normalized.startsWith("ABILITY:") || normalized.startsWith("יכולת:")
                    || normalized.startsWith("יכולת אקטיבית:")) {
                isHeader = true;
            } else {
                String[] markers = {
                        "RIGHT CLICK", "מקש ימני", "LCLICK", "מקש שמאלי", "מקס ימני", "לחיצה ימנית", "לחיצה שמאלית",
                        "שיפט + לחיצה ימנית", "שיפט + לחיצה שמאלית", "פעיל", "פגיעה באויב", "קבלת פגיעה", "בהריגה",
                        "חציבה", "עיבוד אדמה", "חקלאות", "בשיפט", "בקפיצה", "פגיעה עם חץ", "עמידה בשמש", "שהייה במים",
                        "יכולת פסיבית"
                };
                for (String s : markers) {
                    if (normalized.contains(normalizeForMatch(s))) {
                        isHeader = true;
                        break;
                    }
                }
            }

            if (isHeader) {
                if (currentAbility != null && !currentAbility.isEmpty()) {
                    abilities.add(currentAbility);
                }
                currentAbility = new ArrayList<>();
                currentAbility.add(line);
            } else if (currentAbility != null) {
                // STOP if we hit a known rarity
                boolean isRarityLine = false;
                Collection<Rarity> allR = plugin.getRarityManager().getAllRarities();
                if (allR != null) {
                    for (Rarity r : allR) {
                        if (r != null && r.getDisplayName() != null) {
                            String rStripped = dev.agam.skyblockitems.enchantsystem.utils.ColorUtils
                                    .stripColor(r.getDisplayName()).trim();
                            if (stripped.equalsIgnoreCase(rStripped)) {
                                isRarityLine = true;
                                break;
                            }
                        }
                    }
                }

                // Description stop condition check
                // Only stop if it's an empty line, a rarity line, or a STAT line (Label: Value)
                // We DON'T stop just because there are numbers (common in abilities)
                boolean isStatLine = stripped.contains(":") && (stripped.contains("%") || stripped.matches(".*\\d+.*"));

                if (isFooter || isRarityLine || isStatLine) {
                    abilities.add(currentAbility);
                    currentAbility = null;
                } else {
                    currentAbility.add(line);
                }
            }
        }
        if (currentAbility != null && !currentAbility.isEmpty()) {
            abilities.add(currentAbility);
        }

        // Deduplicate abilities by stripped header name (case-insensitive)
        Map<String, List<String>> deduplicated = new LinkedHashMap<>();
        for (List<String> block : abilities) {
            if (block.isEmpty())
                continue;
            String header = dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.stripColor(block.get(0)).trim()
                    .toUpperCase();
            if (!deduplicated.containsKey(header) || block.size() > deduplicated.get(header).size()) {
                deduplicated.put(header, block);
            }
        }

        return new ArrayList<>(deduplicated.values());
    }

    private void restoreAbilitiesFromNBT(ItemStack item, List<List<String>> preservedAbilities) {
        NBTItem nbt = NBTItem.get(item);
        Set<String> existingHeaders = preservedAbilities.stream()
                .map(block -> ColorUtils.stripColor(block.get(0)).trim().toUpperCase())
                .collect(Collectors.toSet());

        for (String tag : nbt.getTags()) {
            if (tag.startsWith("SKYBLOCK_")) {
                String abilityId = tag.replace("SKYBLOCK_", "");
                // Resolve the ability object
                dev.agam.skyblockitems.abilities.SkyBlockAbility ability = plugin.getAbilityManager()
                        .getAbility(abilityId);
                if (ability == null)
                    ability = plugin.getAbilityManager().getAbilityByName(abilityId);

                if (ability != null) {
                    String cleanName = ColorUtils.stripColor(ability.getDisplayName()).trim().toUpperCase();
                    if (existingHeaders.stream().anyMatch(h -> h.contains(cleanName)))
                        continue;

                    // Reconstruct the ability block
                    List<String> reconstructed = new ArrayList<>();
                    String params = nbt.getString(tag);
                    String[] split = params.split("\\s+");

                    Map<String, String> placeholders = new HashMap<>();
                    if (split.length > 0)
                        placeholders.put("cooldown", split[0]);
                    if (split.length > 1)
                        placeholders.put("mana", split[1]);
                    if (split.length > 2)
                        placeholders.put("damage", split[2]);
                    if (split.length > 3)
                        placeholders.put("range", split[3]);

                    // Defaults for description placeholders if not in params
                    placeholders.putIfAbsent("cooldown", String.valueOf(ability.getDefaultCooldown()));
                    placeholders.putIfAbsent("mana", String.valueOf(ability.getDefaultManaCost()));
                    placeholders.putIfAbsent("damage", String.valueOf(ability.getDefaultDamage()));
                    placeholders.putIfAbsent("range", String.valueOf(ability.getDefaultRange()));

                    // Build Header
                    String headerFormat = plugin.getAbilitiesConfig().getString("ability-header-format",
                            "&7{ability} &f{trigger}");
                    String header = ColorUtils.colorize(headerFormat
                            .replace("{ability}", ability.getDisplayName())
                            .replace("{trigger}", ability.getDefaultTrigger().getDisplayName()));
                    reconstructed.add(header);

                    // Build Description
                    reconstructed.addAll(ability.getDescriptionWithPlaceholders(placeholders));

                    preservedAbilities.add(reconstructed);
                    existingHeaders.add(ColorUtils.stripColor(header).trim().toUpperCase());
                }
            }
        }
    }

    private void applyReforgeMMOItems(ItemStack item, Reforge reforge, String savedOriginalName, String oldReforgeId) {
        LiveMMOItem mmoItem = new LiveMMOItem(item);
        ItemMeta itemMeta = item.getItemMeta();
        List<String> preMutationLore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();

        try {
            List<List<String>> preservedAbilities = extractAbilitiesRaw(preMutationLore);
            restoreAbilitiesFromNBT(item, preservedAbilities);

            // 1. SUBTRACT old stats from PDC receipt
            if (pdc.has(PDC_STATS_RECEIPT, PersistentDataType.STRING)) {
                String receiptStr = pdc.get(PDC_STATS_RECEIPT, PersistentDataType.STRING);
                Map<String, Double> receipt = deserializeStats(receiptStr);
                // Check stats before removal
                statApplier.removeStatsFromReceipt(item, mmoItem, receipt);

                // Commit changes and re-wrap
                ItemStack tmp = mmoItem.newBuilder().buildSilently();
                item.setItemMeta(tmp.getItemMeta());
                mmoItem = new LiveMMOItem(item); // Re-wrap with clean state
            }

            // 1.5 SUBTRACT old enchantments from PDC receipt
            if (pdc.has(PDC_ENCHANTS, PersistentDataType.STRING)) {
                String enchantsStr = pdc.get(PDC_ENCHANTS, PersistentDataType.STRING);
                if (enchantsStr != null && !enchantsStr.isEmpty()) {
                    for (String enchantData : enchantsStr.split(",")) {
                        String[] split = enchantData.split(":");
                        if (split.length >= 2) {
                            try {
                                Enchantment e = Enchantment.getByKey(NamespacedKey.minecraft(split[0].toLowerCase()));
                                if (e != null && itemMeta.hasEnchant(e)) {
                                    itemMeta.removeEnchant(e);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }

            // 1.6 REMOVE old abilities from PDC receipt
            Set<String> oldReforgeAbilities = new HashSet<>(); // Hoisted for Lore filtering
            if (pdc.has(PDC_ABILITIES, PersistentDataType.STRING)) {
                String abilitiesStr = pdc.get(PDC_ABILITIES, PersistentDataType.STRING);
                if (abilitiesStr != null && !abilitiesStr.isEmpty()) {
                    oldReforgeAbilities.addAll(Arrays.asList(abilitiesStr.split(",")));
                    // Consistency removal from memory item
                    if (mmoItem.hasData(ItemStats.ABILITIES)) {
                        AbilityListData abilityList = (AbilityListData) mmoItem.getData(ItemStats.ABILITIES);
                        abilityList.getAbilities().removeIf(
                                ad -> oldReforgeAbilities.contains(ad.getHandler().getId().toUpperCase()));
                        mmoItem.setData(ItemStats.ABILITIES, abilityList);
                    }
                }
            }

            // 1.7 STAT RESET via StatApplier (Handles DoubleData correctly)
            // No direct NBT subtraction here as it interferes with MMOItems' template
            // baseline.
            // Wrap is already done above.

            // 2. MERGE new stats into LiveMMOItem
            Map<String, Double> addedStats = new HashMap<>();

            // Get data based on item rarity
            Rarity itemRarity = plugin.getRarityManager().getRarityForItem(item, NBTItem.get(item));
            String rarityId = (itemRarity != null) ? itemRarity.getIdentifier() : "COMMON";
            Reforge.RarityData data = reforge.getDataFor(rarityId);

            for (Map.Entry<String, Double> entry : data.getStats().entrySet()) {
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

            // 3. functional abilities removed

            // 4. DO THE BUILD (to get functional stats synced)
            ItemStack builtItem = mmoItem.newBuilder().build();

            // 5. NOW - THE CREATIVE PART: Build lore manually!
            ItemMeta finalMeta = builtItem.getItemMeta();
            if (finalMeta == null)
                return;

            // Get the base lore that MMOItems generated
            List<String> baseLore = finalMeta.hasLore() ? new ArrayList<>(finalMeta.getLore()) : new ArrayList<>();

            // Build the PERFECT lore manually (Structural Preservation)
            Reforge oldReforge = oldReforgeId != null ? plugin.getReforgeManager().getReforge(oldReforgeId) : null;
            List<String> perfectLore = buildPerfectLore(preMutationLore, baseLore, reforge, data, oldReforge,
                    addedStats,
                    preservedAbilities,
                    oldReforgeAbilities);

            // Set the name
            finalMeta.setDisplayName(ColorUtils.colorize(reforge.getDisplayName() + " " + savedOriginalName));

            // Set the perfect lore
            finalMeta.setLore(perfectLore);

            // Apply enchantments and track them
            List<String> addedEnchantsList = new ArrayList<>();
            for (String s : data.getEnchants()) {
                try {
                    String[] split = s.split(":");
                    Enchantment e = Enchantment.getByName(split[0].toUpperCase());
                    if (e == null)
                        e = Enchantment.getByKey(NamespacedKey.minecraft(split[0].toLowerCase()));
                    if (e != null) {
                        int level = Integer.parseInt(split[1]);
                        finalMeta.addEnchant(e, level, true);
                        addedEnchantsList.add(e.getKey().getKey() + ":" + level);
                    }
                } catch (Exception ignored) {
                }
            }

            // Save PDC
            PersistentDataContainer finalPdc = finalMeta
                    .getPersistentDataContainer();
            finalPdc.set(PDC_REFORGE_ID, PersistentDataType.STRING, reforge.getId());
            finalPdc.set(PDC_STATS_RECEIPT, PersistentDataType.STRING,

                    serializeStats(addedStats));
            finalPdc.set(PDC_ABILITIES, PersistentDataType.STRING, "");
            finalPdc.set(PDC_ENCHANTS, PersistentDataType.STRING, String.join(",", addedEnchantsList));
            finalPdc.set(PDC_ORIGINAL_NAME, PersistentDataType.STRING, savedOriginalName);

            // FINAL SYNC - ONE TIME
            builtItem.setItemMeta(finalMeta);
            item.setType(builtItem.getType());
            item.setAmount(builtItem.getAmount());
            item.setItemMeta(finalMeta);

        } catch (Throwable e) {
            plugin.getLogger().severe("[SkyBlock Reforge] Manual Build Fail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Builds the PERFECT lore with (+X) annotations and ability descriptions.
     * This is the creative solution - we construct exactly what we want!
     */
    private List<String> buildPerfectLore(List<String> rawPreMutationLore, List<String> builtLore, Reforge reforge,
            Reforge.RarityData data,
            Reforge oldReforge,
            Map<String, Double> addedStats, List<List<String>> preservedAbilities, Set<String> oldReforgeAbilities) {

        List<String> result = new ArrayList<>(builtLore);

        // 1. ANNOTATE EXISTING STATS
        Map<String, String> statAnnotations = new HashMap<>();
        String statFormat = plugin.getReforgeManager().getStatFormat();
        for (Map.Entry<String, Double> entry : addedStats.entrySet()) {
            double value = entry.getValue();
            String prefix = value >= 0 ? "+" : "";
            String strVal = (value % 1 == 0) ? String.valueOf((long) value) : String.valueOf(value);
            statAnnotations.put(entry.getKey(),
                    " " + ColorUtils.colorize(statFormat.replace("{value}", prefix + strVal)));
        }

        Set<String> injectedStats = new HashSet<>();
        for (int i = 0; i < result.size(); i++) {
            String line = result.get(i);
            String stripped = ColorUtils.stripColor(line).trim();
            if (stripped.contains(":") && (stripped.contains("%") || stripped.matches(".*\\d+.*"))) {
                String label = stripped.split(":")[0].trim().toUpperCase();

                for (String statId : statAnnotations.keySet()) {
                    if (injectedStats.contains(statId))
                        continue;
                    String upperStatId = statId.toUpperCase();
                    boolean match = false;
                    if (upperStatId.contains("ATTACK_DAMAGE")
                            && (label.contains("נזק") || label.contains("DAMAGE") || label.contains("ATTACK")))
                        match = true;
                    else if (upperStatId.contains("PROJECTILE_DAMAGE")
                            && (label.contains("קליעים") || label.contains("חץ") || label.contains("PROJECTILE")
                                    || label.contains("ARROW")))
                        match = true;
                    else if (upperStatId.contains("CRITICAL_STRIKE_CHANCE")
                            && (label.contains("סיכוי") || label.contains("CRITICAL") || label.contains("CHANCE")))
                        match = true;
                    else if (upperStatId.contains("CRITICAL_STRIKE_POWER")
                            && (label.contains("עוצמת") || label.contains("POWER") || label.contains("STRENGTH")))
                        match = true;
                    else if (upperStatId.contains("HEALTH")
                            && (label.contains("חיים") || label.contains("HEALTH") || label.contains("HP")))
                        match = true;
                    else if (upperStatId.contains("MAX_MANA")
                            && (label.contains("מאנה") || label.contains("MANA") || label.contains("INTELLIGENCE")))
                        match = true;
                    else if (upperStatId.contains("DEFENSE")
                            && (label.contains("הגנה") || label.contains("DEFENSE") || label.contains("ARMOR")))
                        match = true;
                    else if ((upperStatId.contains("MOVEMENT_SPEED") || upperStatId.contains("SPEED"))
                            && (label.contains("מהירות") || label.contains("SPEED") || label.contains("WALK")))
                        match = true;

                    if (!match && (label.contains(upperStatId) || upperStatId.contains(label)))
                        match = true;

                    if (match) {
                        result.set(i, line + statAnnotations.get(statId));
                        injectedStats.add(statId);
                        break;
                    }
                }
            }
        }

        // 2. RESTORE ABILITY DESCRIPTIONS (SURGICALLY)
        if (preservedAbilities != null && !preservedAbilities.isEmpty()) {
            for (List<String> pBlock : preservedAbilities) {
                if (pBlock.isEmpty())
                    continue;
                String pHeaderRegex = ".*" + Pattern.quote(ColorUtils.stripColor(pBlock.get(0)).trim()) + ".*";

                for (int i = 0; i < result.size(); i++) {
                    String lineContent = ColorUtils.stripColor(result.get(i)).trim();
                    if (lineContent.matches(pHeaderRegex)) {
                        // Found the header. Replace descriptions below it.
                        int end = i + 1;
                        while (end < result.size()) {
                            String nextStripped = ColorUtils.stripColor(result.get(end)).trim();
                            if (nextStripped.isEmpty())
                                break;
                            String nextUpper = nextStripped.toUpperCase();
                            if (nextUpper.contains("ABILITY:") || nextUpper.contains("יכולת:") ||
                                    nextUpper.contains("RIGHT CLICK") || nextUpper.contains("LCLICK") ||
                                    nextUpper.contains("פעיל"))
                                break;

                            boolean isRarity = false;
                            for (Rarity r : plugin.getRarityManager().getAllRarities()) {
                                if (nextUpper.equals(ColorUtils.stripColor(r.getDisplayName()).trim().toUpperCase())) {
                                    isRarity = true;
                                    break;
                                }
                            }
                            if (isRarity)
                                break;
                            if (nextUpper.contains("REQUIRED") || nextUpper.contains("דרוש")
                                    || nextUpper.contains("רמה"))
                                break;

                            end++;
                        }
                        // Replace
                        for (int j = 0; j < (end - i); j++)
                            result.remove(i);
                        result.addAll(i, pBlock);
                        break;
                    }
                }
            }
        }

        // 3. REMOVE DUPLICATE REQUIREMENTS
        Set<String> seenRequirements = new HashSet<>();
        for (int i = 0; i < result.size(); i++) {
            String line = result.get(i);
            String stripped = ColorUtils.stripColor(line).trim().toUpperCase();
            if (stripped.contains("REQUIRED") || stripped.contains("דרוש") || stripped.contains("רמה")) {
                if (seenRequirements.contains(stripped)) {
                    result.remove(i);
                    i--;
                } else {
                    seenRequirements.add(stripped);
                }
            }
        }

        return result;
    }

    private String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }

    /**
     * Wipes any existing REFORGE ability descriptions or annotations to prevent
     * pollution.
     * CRITICAL: Only removes OLD reforge data, NOT all abilities!
     */
    private void cleanLore(List<String> lore, Reforge reforge, Reforge oldReforge, Set<String> oldReforgeAbilities) {
        Set<String> namesToRemove = new HashSet<>();

        // Add enchant names from old reforge (check all tiers)
        if (oldReforge != null) {
            Set<String> allOldEnchants = new HashSet<>();
            for (Reforge.RarityData tier : oldReforge.getRarityDataMap().values()) {
                allOldEnchants.addAll(tier.getEnchants());
            }

            for (String s : allOldEnchants) {
                try {
                    String enchantId = s.split(":")[0].toUpperCase();
                    String displayName = plugin.getEnchantManager().getDisplayNameForId(enchantId.toLowerCase());
                    if (displayName == null || displayName.isEmpty())
                        continue;

                    if (displayName.equalsIgnoreCase(enchantId)) {
                        displayName = Arrays.stream(enchantId.split("_"))
                                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                                .collect(Collectors.joining(" "));
                    } else {
                        displayName = ColorUtils.stripColor(ColorUtils.colorize(displayName));
                    }
                    namesToRemove.add(normalizeForMatch(displayName));
                } catch (Exception ignored) {
                }
            }
        }

        if (oldReforgeAbilities != null) {
            for (String id : oldReforgeAbilities) {
                SkyBlockAbility a = this.plugin.getAbilityManager().getAbility(id);
                if (a != null)
                    namesToRemove.add(normalizeForMatch(a.getDisplayName()));
            }
        }

        // Regex for reforge annotations: ( +X ), ( X+ ), ( -X ), etc.
        Pattern STAT_PATTERN = Pattern.compile("\\s*\\([^()]*[+-]\\d+[^()]*\\)");

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String strippedRaw = dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.stripColor(line).trim();
            if (strippedRaw.isEmpty())
                continue; // SKIP EMPTY LINES

            String stripped = normalizeForMatch(strippedRaw);

            // 1. Remove reforge annotations from existing lines (preserves the base
            // line/position)
            Matcher m = STAT_PATTERN.matcher(line);
            if (m.find()) {
                lore.set(i, m.replaceAll(""));
                continue;
            }

            // 2. Remove Ability/Enchant Headers for Reforge Data
            boolean isDataToRemove = false;
            for (String name : namesToRemove) {
                // For enchants, we match "§7" + Name + " ROMAN"
                // But normalizeForMatch strips color and uppers.
                if (stripped.contains(name) || name.contains(stripped)) {
                    // Only remove if it's a short "header-like" line or exactly matches reforge
                    // format
                    if (stripped.length() < name.length() + 15) {
                        isDataToRemove = true;
                        break;
                    }
                }
            }
            if (isDataToRemove) {
                lore.remove(i);
                i--;
            }
        }
    }

    private String normalizeForMatch(String text) {
        if (text == null)
            return "";
        // Normalize Unicode (NFC), remove color codes, strip whitespace, and uppercase
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        return normalized.trim().toUpperCase();
    }

    private void resetMMOItem(ItemStack item) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                return;
            org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

            if (!pdc.has(PDC_STATS_RECEIPT, org.bukkit.persistence.PersistentDataType.STRING) &&
                    !pdc.has(PDC_ABILITIES, org.bukkit.persistence.PersistentDataType.STRING))
                return;

            LiveMMOItem mmoItem = new LiveMMOItem(item);

            // 1. Remove Tracked Stats (Safe Subtraction)
            if (pdc.has(PDC_STATS_RECEIPT, org.bukkit.persistence.PersistentDataType.STRING)) {
                String receiptStr = pdc.get(PDC_STATS_RECEIPT, org.bukkit.persistence.PersistentDataType.STRING);
                Map<String, Double> trackedStats = deserializeStats(receiptStr);
                statApplier.removeStatsFromReceipt(item, mmoItem, trackedStats);
            }

            // 2. Remove Tracked Abilities
            if (pdc.has(PDC_ABILITIES, org.bukkit.persistence.PersistentDataType.STRING)
                    && mmoItem.hasData(ItemStats.ABILITIES)) {
                String abilitiesStr = pdc.get(PDC_ABILITIES, org.bukkit.persistence.PersistentDataType.STRING);
                if (abilitiesStr != null && !abilitiesStr.isEmpty()) {
                    Set<String> toRemove = Arrays.stream(abilitiesStr.split(",")).collect(Collectors.toSet());
                    AbilityListData abilityList = (AbilityListData) mmoItem.getData(ItemStats.ABILITIES);
                    abilityList.getAbilities().removeIf(ad -> toRemove.contains(ad.getHandler().getId().toUpperCase()));
                    mmoItem.setData(ItemStats.ABILITIES, abilityList);
                }
            }

            // 3. Rebuild to clean lore/NBT
            ItemStack cleaned = mmoItem.newBuilder().build();

            // 4. Clean Reforge Tags from Meta
            ItemMeta finalMeta = cleaned.getItemMeta();
            if (finalMeta != null) {
                org.bukkit.persistence.PersistentDataContainer finalPdc = finalMeta.getPersistentDataContainer();
                finalPdc.remove(PDC_REFORGE_ID);
                finalPdc.remove(PDC_STATS_RECEIPT);
                finalPdc.remove(PDC_ABILITIES);

                // Restore original name if available
                if (pdc.has(PDC_ORIGINAL_NAME, org.bukkit.persistence.PersistentDataType.STRING)) {
                    finalMeta.setDisplayName(
                            pdc.get(PDC_ORIGINAL_NAME, org.bukkit.persistence.PersistentDataType.STRING));
                }

                cleaned.setItemMeta(finalMeta);
            }

            item.setType(cleaned.getType());
            item.setItemMeta(cleaned.getItemMeta());

        } catch (Exception e) {
            plugin.getLogger().warning("[Reforge] PDC Reset failed: " + e.getMessage());
        }
    }

    private void updateItemName(ItemMeta meta, Reforge reforge, String savedOriginalName) {
        // Simple name rule: [Reforge] [OriginalColors][OriginalName]
        meta.setDisplayName(ColorUtils.colorize(reforge.getDisplayName() + " " + savedOriginalName));
    }

    private void applyManualBonusAnnotations(List<String> lore, Reforge reforge) {
        // Map of display name components -> (StatID, bonus value)
        Map<String, Map.Entry<String, Double>> bonuses = new HashMap<>();
        // Use all unique stats across all rarities for annotation matching
        Set<String> allStatKeys = new HashSet<>();
        for (Reforge.RarityData tier : reforge.getRarityDataMap().values()) {
            allStatKeys.addAll(tier.getStats().keySet());
        }

        for (String statKey : allStatKeys) {
            String statId = statKey.replace("mmoitems_", "").toUpperCase().replace("-", "_");
            String statDisplayName = getStatDisplayName(statId);
            if (statDisplayName != null) {
                // Determine a value to show (average or from first tier)
                double value = reforge.getDataFor("COMMON").getStats().getOrDefault(statKey, 0.0);
                bonuses.put(ColorUtils.stripColor(statDisplayName).trim(),
                        new AbstractMap.SimpleEntry<>(statId, value));
            }
        }

        // Loop through each line of lore: if (line.contains(statDisplayName))
        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String strippedLine = ColorUtils.stripColor(line);

            for (Map.Entry<String, Map.Entry<String, Double>> bonusEntry : bonuses.entrySet()) {
                String statName = bonusEntry.getKey();

                if (strippedLine.contains(statName)) {
                    // Safety: prevent double injection on the same line
                    if (strippedLine.contains("(+"))
                        continue;

                    String statId = bonusEntry.getValue().getKey();
                    double value = bonusEntry.getValue().getValue();
                    String strVal = (value % 1 == 0) ? String.valueOf((long) value) : String.valueOf(value);

                    // Pull symbol and construct bonus string
                    String symbol = plugin.getReforgeManager().getStatSymbol(statId);
                    String color = value >= 0 ? "§a" : "§c";
                    String prefix = value >= 0 ? "+" : "";

                    // Exact format: lore.set(i, line + " §a(" + symbol + " +" + value + ")");
                    String annotation = " " + color + "(" + symbol + " " + prefix + strVal + ")";
                    lore.set(i, line + annotation);
                    break;
                }
            }
        }
    }

    // injectManualAbilityLore removed

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
        if (item == null || !item.hasItemMeta())
            return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return null;
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(PDC_REFORGE_ID, org.bukkit.persistence.PersistentDataType.STRING);
    }

    public boolean hasReforge(ItemStack item) {
        return getCurrentReforge(item) != null;
    }

    private void applyReforgeLegacy(ItemStack item, Reforge reforge, String itemType, Reforge.RarityData data) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(PDC_REFORGE_ID, PersistentDataType.STRING, reforge.getId());
            item.setItemMeta(meta);
        }
        statApplier.applyStats(item, data, itemType);
    }

    private void applyEnchantmentsLegacy(ItemStack item, Reforge.RarityData data) {
        for (String s : data.getEnchants()) {
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
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 1. Restore Original Name from PDC
        if (pdc.has(PDC_ORIGINAL_NAME, org.bukkit.persistence.PersistentDataType.STRING)) {
            meta.setDisplayName(pdc.get(PDC_ORIGINAL_NAME, org.bukkit.persistence.PersistentDataType.STRING));
        }

        // 2. Clean PDC tags
        pdc.remove(PDC_REFORGE_ID);
        pdc.remove(PDC_STATS_RECEIPT);
        pdc.remove(PDC_ABILITIES);

        item.setItemMeta(meta);

        // 3. Remove stats
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
