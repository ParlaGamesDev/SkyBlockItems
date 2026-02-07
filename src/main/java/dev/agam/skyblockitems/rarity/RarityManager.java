package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.rarity.ItemRarityConfigManager.CustomRule;
import dev.agam.skyblockitems.rarity.ItemRarityConfigManager.RarityDefinition;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RarityManager {

    private final SkyBlockItems plugin;
    private final ItemRarityConfigManager configManager;
    private final String RARITY_KEY = "skyblock.rarity";

    public RarityManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.configManager = new ItemRarityConfigManager(plugin);
    }

    public ItemRarityConfigManager getConfigManager() {
        return configManager;
    }

    public String getRarityId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return "COMMON";

        NBTItem nbt = NBTItem.get(item);

        // 1. Check NBT Tag (Highest Priority)
        if (nbt.hasTag(RARITY_KEY)) {
            String val = nbt.getString(RARITY_KEY);
            if (configManager.getRarity(val) != null) {
                return val;
            }
            if (configManager.getRarity(val.toUpperCase()) != null) {
                return val.toUpperCase();
            }
        }

        // 2. Custom Rules
        for (CustomRule rule : configManager.getCustomRules()) {
            if (matchesRule(item, rule)) {
                if (rule.noRarity)
                    return null;
                return rule.targetRarityId;
            }
        }

        // 3. Default
        return "COMMON";
    }

    private boolean matchesRule(ItemStack item, CustomRule rule) {
        // 1. MMOItems Check (Specific Item)
        if (rule.mmoItemType != null && rule.mmoItemId != null) {
            NBTItem nbt = NBTItem.get(item);
            if (!nbt.hasTag("MMOITEMS_ITEM_TYPE") || !nbt.hasTag("MMOITEMS_ITEM_ID")) {
                return false;
            }
            String type = nbt.getString("MMOITEMS_ITEM_TYPE");
            String id = nbt.getString("MMOITEMS_ITEM_ID");

            return type.equalsIgnoreCase(rule.mmoItemType) && id.equalsIgnoreCase(rule.mmoItemId);
        }

        // 2. Material Check (Generic)
        if (rule.material != null && item.getType() != rule.material) {
            return false;
        }
        return true;
    }

    public ItemStack setRarity(ItemStack item, String rarityId) {
        if (item == null || item.getType() == Material.AIR)
            return item;
        if (configManager.getRarity(rarityId) == null)
            return item;

        NBTItem nbt = NBTItem.get(item);
        nbt.addTag(new ItemTag(RARITY_KEY, rarityId));

        ItemStack newItem = nbt.toItem();
        updateLore(newItem);
        return newItem;
    }

    public void updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return;

        String rarityId = getRarityId(item);

        // Handle No Rarity Case
        if (rarityId == null) {
            // We need to CLEANUP rarity lines if they exist.
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasLore())
                return;

            List<String> lore = meta.getLore();
            List<String> newLore = new ArrayList<>();
            boolean changed = false;

            for (String line : lore) {
                if (!isRarityLine(line)) {
                    newLore.add(line);
                } else {
                    changed = true;
                }
            }

            // Remove trailing empty lines (The "Spacing" fix)
            while (!newLore.isEmpty() && newLore.get(newLore.size() - 1).trim().isEmpty()) {
                newLore.remove(newLore.size() - 1);
                changed = true;
            }

            if (changed) {
                meta.setLore(newLore);
                item.setItemMeta(meta);
            }
            return;
        }

        RarityDefinition def = configManager.getRarity(rarityId);
        if (def == null)
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        // 1. Construct the expected rarity lines for THIS item's rarity
        List<String> expectedSuffix = new ArrayList<>();
        for (String formatLine : configManager.getLoreFormat()) {
            if (formatLine.contains("{lore}"))
                continue;
            if (formatLine.isEmpty()) {
                expectedSuffix.add("");
                continue;
            }
            String formatted = formatLine.replace("{rarity}", def.displayName)
                    .replace("{rarity-prefix}", def.displayName)
                    .replace("{rarity-name}", def.identifier);
            expectedSuffix.add(ChatColor.translateAlternateColorCodes('&', formatted));
        }

        // 2. Check if the item already ends with these exact lines
        if (lore.size() >= expectedSuffix.size()) {
            List<String> currentSuffix = lore.subList(lore.size() - expectedSuffix.size(), lore.size());
            if (currentSuffix.equals(expectedSuffix)) {
                // Already updated! logic: check if the line BEFORE the suffix is a rarity line
                // (meaning we might have double applied?)
                // Actually, just checking suffix is usually enough if we trust our strippers.
                // But wait, if we switch rarity, the suffix changes.
                // If the suffix matches, we are good.
                return;
            }
        }

        // Check Custom Rules for BOTH Rarity and Lore Application
        CustomRule matchedRule = null;
        for (CustomRule rule : configManager.getCustomRules()) {
            if (matchesRule(item, rule)) {
                matchedRule = rule;
                break;
            }
        }

        List<String> preservedLore = new ArrayList<>();

        // LOGIC: If matched rule has lore, use it (Overwriting item lore).
        if (matchedRule != null && !matchedRule.lore.isEmpty()) {
            preservedLore.addAll(matchedRule.lore);
        } else {
            // Standard preservation
            for (String line : lore) {
                if (!isRarityLine(line)) {
                    preservedLore.add(line);
                }
            }
        }

        // Remove trailing empty lines from preserved lore
        while (!preservedLore.isEmpty() && preservedLore.get(preservedLore.size() - 1).trim().isEmpty()) {
            preservedLore.remove(preservedLore.size() - 1);
        }

        List<String> loreBuilder = new ArrayList<>();

        // Append new rarity lore
        for (String formatLine : configManager.getLoreFormat()) {
            // Handle {lore} placeholder
            if (formatLine.contains("{lore}")) {
                loreBuilder.addAll(preservedLore);
                continue;
            }

            if (formatLine.isEmpty()) {
                loreBuilder.add("");
                continue;
            }
            String formatted = formatLine.replace("{rarity}", def.displayName)
                    .replace("{rarity-prefix}", def.displayName)
                    .replace("{rarity-name}", def.identifier);
            loreBuilder.add(ChatColor.translateAlternateColorCodes('&', formatted));
        }

        meta.setLore(loreBuilder);
        item.setItemMeta(meta);
    }

    public boolean isRarityLine(String line) {
        // Prepare comparison versions of the line
        String stripped = ChatColor.stripColor(line);

        for (RarityDefinition def : configManager.getRarities()) {
            for (String format : configManager.getLoreFormat()) {
                if (format.isEmpty())
                    continue;

                String expected = format.replace("{rarity}", def.displayName)
                        .replace("{rarity-prefix}", def.displayName)
                        .replace("{rarity-name}", def.identifier);

                String expectedColor = ChatColor.translateAlternateColorCodes('&', expected);
                String expectedStripped = ChatColor.stripColor(expectedColor);

                if (line.equals(expectedColor) || stripped.equals(expectedStripped)) {
                    return true;
                }
            }
        }
        return false;
    }
}
