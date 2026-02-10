package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.rarity.Rarity;
import dev.agam.skyblockitems.rarity.RarityManager;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Handles the application and removal of reforges from items.
 * Manages item name changes, stat application, enchantments, abilities, and
 * rarity upgrades.
 */
public class ReforgeApplier {

    private static final String NBT_REFORGE_KEY = "skyblock.reforge";
    private static final String NBT_ORIGINAL_NAME_KEY = "skyblock.original_name";
    private static final Pattern COLOR_CODE_PATTERN = Pattern
            .compile("(&[0-9a-fk-or]|&#[0-9a-fA-F]{6}|<#[0-9a-fA-F]{6}>|<gradient:[^>]+>)");

    private final SkyBlockItems plugin;
    private final StatApplier statApplier;
    private final NamespacedKey reforgeKey;
    private final NamespacedKey originalNameKey;

    public ReforgeApplier(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.statApplier = new StatApplier(plugin);
        this.reforgeKey = new NamespacedKey(plugin, NBT_REFORGE_KEY);
        this.originalNameKey = new NamespacedKey(plugin, NBT_ORIGINAL_NAME_KEY);
    }

    /**
     * Applies a reforge to an item.
     * 
     * @param item     The item to reforge
     * @param reforge  The reforge to apply
     * @param itemType The type of item (for validation)
     * @return true if the reforge was successfully applied
     */
    public boolean applyReforge(ItemStack item, Reforge reforge, String itemType) {
        if (item == null || reforge == null || !item.hasItemMeta()) {
            return false;
        }

        // Remove existing reforge if present
        String currentReforge = getCurrentReforge(item);
        if (currentReforge != null) {
            Reforge oldReforge = plugin.getReforgeManager().getReforge(currentReforge);
            if (oldReforge != null) {
                removeReforge(item, oldReforge);
            }
        }

        ItemMeta meta = item.getItemMeta();

        // Store original name in NBT if not already present
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(originalNameKey, PersistentDataType.STRING)) {
            String originalName = meta.hasDisplayName() ? meta.getDisplayName() : "";
            container.set(originalNameKey, PersistentDataType.STRING, originalName);
        }

        // Store reforge ID in PersistentDataContainer
        container.set(reforgeKey, PersistentDataType.STRING, reforge.getId());

        // Apply item name change with reforge prefix
        updateItemName(meta, reforge);

        // Apply item meta first (needed for NBT operations)
        item.setItemMeta(meta);

        // Apply stats
        statApplier.applyStats(item, reforge, itemType);

        // Apply enchantments
        applyEnchantments(item, reforge);

        // Apply abilities
        applyAbilities(item, reforge);

        // Handle rarity upgrade
        upgradeRarity(item, reforge);

        // Final lore update to show reforge effects
        updateItemLore(item, reforge);

        return true;
    }

    /**
     * Removes a reforge from an item.
     * 
     * @param item    The item to remove reforge from
     * @param reforge The reforge to remove
     */
    public void removeReforge(ItemStack item, Reforge reforge) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        // Remove reforge ID from PersistentDataContainer
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(reforgeKey);

        // Restore original item name from NBT
        restoreItemName(meta, reforge);

        // Remove reforge lore
        removeReforgeLore(meta);

        item.setItemMeta(meta);

        // Remove stats
        statApplier.removeStats(item, reforge);

        // Note: We don't remove enchantments/abilities as they may come from other
        // sources
    }

    /**
     * Gets the current reforge ID from an item.
     * 
     * @param item The item to check
     * @return The reforge ID, or null if no reforge
     */
    public String getCurrentReforge(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(reforgeKey, PersistentDataType.STRING);
    }

    /**
     * Checks if an item has a reforge.
     * 
     * @param item The item to check
     * @return true if the item has a reforge
     */
    public boolean hasReforge(ItemStack item) {
        return getCurrentReforge(item) != null;
    }

    /**
     * Updates the item name to include the reforge prefix.
     * Preserves the original color codes.
     */
    private void updateItemName(ItemMeta meta, Reforge reforge) {
        String originalName = meta.hasDisplayName() ? meta.getDisplayName() : "";

        // If the item already has a reforge in the name, remove it first
        String cleanedName = removeReforgePrefix(originalName);

        // Extract original color codes
        String originalColors = extractLeadingColors(cleanedName);

        // Remove leading colors from the cleaned name
        String nameWithoutColors = cleanedName.replaceFirst("^(" + COLOR_CODE_PATTERN.pattern() + ")+", "");

        // Build new name: [Reforge Color][Reforge Name] [Original Colors][Original
        // Name]
        String newName;
        if (nameWithoutColors.isEmpty()) {
            newName = reforge.getDisplayName();
        } else {
            newName = reforge.getDisplayName() + " " + originalColors + nameWithoutColors;
        }

        meta.setDisplayName(ColorUtils.colorize(newName));
    }

    /**
     * Updates the item's lore to display reforge stats.
     */
    private void updateItemLore(ItemStack item, Reforge reforge) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        // Clean up old reforge lore first
        removeReforgeLore(meta);

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        // Find the index of the rarity line to insert above it
        int insertIndex = -1;
        RarityManager rarityManager = plugin.getRarityManager();
        List<String> rarityNames = new ArrayList<>();
        if (rarityManager != null) {
            for (Rarity r : rarityManager.getAllRarities()) {
                rarityNames.add(ColorUtils.stripColor(ColorUtils.colorize(r.getDisplayName())).toUpperCase().trim());
                rarityNames.add(r.getIdentifier().toUpperCase().trim());
            }
        }

        for (int i = 0; i < lore.size(); i++) {
            String line = ColorUtils.stripColor(lore.get(i)).toUpperCase().trim();
            if (line.isEmpty())
                continue;

            // Check against registered rarity names or common defaults
            boolean isRarityLine = rarityNames.contains(line) ||
                    line.equals("COMMON") || line.equals("UNCOMMON") || line.equals("RARE") ||
                    line.equals("EPIC") || line.equals("LEGENDARY") || line.equals("MYTHIC");

            if (isRarityLine) {
                insertIndex = i;
                break;
            }
        }

        // If no rarity line found, append to end
        if (insertIndex == -1) {
            insertIndex = lore.size();
        } else {
            // Check if there is an empty line before rarity and move before it
            if (insertIndex > 0 && ColorUtils.stripColor(lore.get(insertIndex - 1)).trim().isEmpty()) {
                insertIndex--;
            }
        }

        List<String> reforgeLore = new ArrayList<>();

        // Add reforge stats to lore in Hypixel style: §7StatName: §a+Value
        if (!reforge.getStats().isEmpty()) {
            boolean firstStat = true;
            for (Map.Entry<String, Double> entry : reforge.getStats().entrySet()) {
                String rawName = entry.getKey().replace("mmoitems_", "").replace("auraskills_", "");
                String statName = formatStatName(rawName);
                String value = (entry.getValue() > 0 ? "+" : "") + formatValue(entry.getValue());

                // Only add an empty line before the FIRST stat if we are inserting into
                // existing lore
                if (firstStat && insertIndex > 0 && !lore.isEmpty()) {
                    reforgeLore.add("");
                    firstStat = false;
                }

                // If statName already has color (from MMOItems API), we don't force §7
                String colorPrefix = (statName.contains("§") || statName.contains("&")) ? "" : "§7";
                reforgeLore.add(ColorUtils.colorize(colorPrefix + statName + ": §a" + value));
            }
        }

        // Add abilities to lore
        if (!reforge.getAbilities().isEmpty()) {
            for (String abilityId : reforge.getAbilities()) {
                SkyBlockAbility ability = plugin.getAbilityManager().getAbility(abilityId);
                if (ability != null) {
                    reforgeLore.add("");
                    reforgeLore.add(ColorUtils.colorize("§6Reforge Ability: " + ability.getDisplayName()));
                    if (ability.getDescription() != null) {
                        for (String desc : ability.getDescription()) {
                            String processed = desc;
                            // Replace placeholders with default values from the ability
                            processed = processed.replace("{mana}", String.valueOf((int) ability.getDefaultManaCost()));
                            processed = processed.replace("{cooldown}", formatValue(ability.getDefaultCooldown()));
                            processed = processed.replace("{damage}", formatValue(ability.getDefaultDamage()));
                            processed = processed.replace("{range}", formatValue(ability.getDefaultRange()));
                            processed = processed.replace("{radius}", formatValue(ability.getDefaultRange()));
                            processed = processed.replace("{duration}", formatValue(ability.getDefaultDuration()));
                            processed = processed.replace("{amplifier}", formatValue(ability.getDefaultPower()));
                            processed = processed.replace("{power}", formatValue(ability.getDefaultPower()));

                            reforgeLore.add(ColorUtils.colorize("§7" + processed));
                        }
                    }
                }
            }
        }

        // Add one empty line after reforge section if not at the very end
        if (!reforgeLore.isEmpty() && insertIndex < lore.size()) {
            reforgeLore.add("");
        }

        // Insert at the calculated index
        lore.addAll(insertIndex, reforgeLore);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Restores the original item name by removing the reforge prefix.
     */
    private void restoreItemName(ItemMeta meta, Reforge reforge) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(originalNameKey, PersistentDataType.STRING)) {
            String originalName = container.get(originalNameKey, PersistentDataType.STRING);
            if (originalName == null || originalName.isEmpty()) {
                meta.setDisplayName(null);
            } else {
                meta.setDisplayName(originalName);
            }
        }
    }

    /**
     * Removes any reforge prefix from a name.
     */
    private String removeReforgePrefix(String name) {
        // Try to find and remove any reforge name prefix
        // Assumes reforge names are one word followed by a space
        String stripped = ColorUtils.stripColor(ColorUtils.colorize(name));

        // If stripped name has at least 2 words, assume first is reforge
        String[] parts = stripped.split(" ", 2);
        if (parts.length >= 2) {
            // Extract everything after the first word in the original colored name
            int firstSpaceIndex = name.indexOf(" ");
            if (firstSpaceIndex > 0 && firstSpaceIndex < name.length() - 1) {
                return name.substring(firstSpaceIndex + 1);
            }
        }

        return name;
    }

    /**
     * Extracts leading color codes from a string.
     */
    private String extractLeadingColors(String text) {
        StringBuilder colors = new StringBuilder();
        Matcher matcher = COLOR_CODE_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            // Only collect consecutive color codes from the start
            if (matcher.start() == lastEnd) {
                colors.append(matcher.group());
                lastEnd = matcher.end();
            } else {
                break;
            }
        }

        return colors.toString();
    }

    /**
     * Applies enchantments from the reforge.
     */
    private void applyEnchantments(ItemStack item, Reforge reforge) {
        for (String enchantString : reforge.getEnchants()) {
            try {
                String[] parts = enchantString.split(":");
                if (parts.length != 2) {
                    plugin.getLogger()
                            .warning("Invalid enchant format in reforge " + reforge.getId() + ": " + enchantString);
                    continue;
                }

                String enchantName = parts[0].toUpperCase();
                int level = Integer.parseInt(parts[1]);

                Enchantment enchant = Enchantment.getByName(enchantName);
                if (enchant == null) {
                    // Try to get it as a key
                    enchant = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase()));
                }

                if (enchant != null) {
                    item.addUnsafeEnchantment(enchant, level);
                } else {
                    plugin.getLogger()
                            .warning("Unknown enchantment in reforge " + reforge.getId() + ": " + enchantName);
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error applying enchantment from reforge " + reforge.getId() + ": "
                        + enchantString + " - " + e.getMessage());
            }
        }
    }

    /**
     * Applies abilities from the reforge by adding them to the item's NBT.
     */
    private void applyAbilities(ItemStack item, Reforge reforge) {
        NBTItem nbtItem = NBTItem.get(item);

        for (String abilityId : reforge.getAbilities()) {
            SkyBlockAbility ability = plugin.getAbilityManager().getAbility(abilityId);
            if (ability == null) {
                plugin.getLogger().warning("Unknown ability in reforge " + reforge.getId() + ": " + abilityId);
                continue;
            }

            // Add ability to the item using NBT
            // This follows the same pattern as the existing ability system
            nbtItem.addTag(new ItemTag("ABILITY_" + abilityId.toUpperCase(), abilityId));
        }

        item.setItemMeta(nbtItem.toItem().getItemMeta());
    }

    /**
     * Upgrades the item's rarity if the reforge provides a higher rarity.
     */
    private void upgradeRarity(ItemStack item, Reforge reforge) {
        RarityManager rarityManager = plugin.getRarityManager();
        if (rarityManager == null) {
            return;
        }

        // Get current rarity
        Rarity currentRarity = rarityManager.getCurrentRarity(item);
        String currentRarityId = (currentRarity != null) ? currentRarity.getIdentifier() : "COMMON";

        // Get reforge rarity
        Rarity reforgeRarity = rarityManager.getRarity(reforge.getRarityUpgrade());
        if (reforgeRarity == null) {
            return;
        }

        // Only upgrade if reforge rarity is higher
        int currentWeight = getRarityWeight(currentRarityId);
        int reforgeWeight = getRarityWeight(reforge.getRarityUpgrade());

        if (reforgeWeight > currentWeight) {
            rarityManager.applyRarity(item, reforgeRarity, false);
        }
    }

    /**
     * Gets the weight of a rarity for comparison.
     */
    private int getRarityWeight(String rarity) {
        return switch (rarity.toUpperCase()) {
            case "COMMON" -> 1;
            case "UNCOMMON" -> 2;
            case "RARE" -> 3;
            case "EPIC" -> 4;
            case "LEGENDARY" -> 5;
            case "MYTHIC" -> 6;
            default -> 1;
        };
    }

    /**
     * Removes reforge-related lines from the item's lore.
     */
    private void removeReforgeLore(ItemMeta meta) {
        if (!meta.hasLore())
            return;

        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty())
            return;

        // Remove lines that look like reforge stats or abilities
        // We look for patterns like "§7StatName: §a+Value" or "§6Reforge Ability:"
        List<String> newLore = new ArrayList<>();
        boolean inAbilitySection = false;

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String stripped = ColorUtils.stripColor(line);

            if (line.contains("§6Reforge Ability:")) {
                inAbilitySection = true;
                continue;
            }

            if (inAbilitySection) {
                if (line.startsWith("§7")) {
                    // This is an ability description line
                    continue;
                } else {
                    inAbilitySection = false;
                }
            }

            // Check if it's a stat line: §7[Stat]: §a[Value]
            // We use a more flexible check for stats that might include icons/colors
            if (line.contains(": §a+")) {
                continue;
            }

            newLore.add(line);
        }

        // Clean up double empty lines and spaces before rarity
        List<String> cleanedLore = new ArrayList<>();
        boolean lastWasEmpty = false;
        for (int i = 0; i < newLore.size(); i++) {
            String line = newLore.get(i);
            boolean currentIsEmpty = ColorUtils.stripColor(line).trim().isEmpty();

            if (currentIsEmpty) {
                if (lastWasEmpty)
                    continue; // No double empty lines

                // If it's an empty line, don't add it if it's the LAST line or followed by
                // rarity
                if (i + 1 < newLore.size()) {
                    String nextLine = ColorUtils.stripColor(newLore.get(i + 1)).toUpperCase().trim();
                    boolean isRarity = nextLine.equals("COMMON") || nextLine.equals("UNCOMMON") ||
                            nextLine.equals("RARE") || nextLine.equals("EPIC") ||
                            nextLine.equals("LEGENDARY") || nextLine.equals("MYTHIC");
                    if (!isRarity) {
                        cleanedLore.add(line);
                        lastWasEmpty = true;
                    }
                }
            } else {
                cleanedLore.add(line);
                lastWasEmpty = false;
            }
        }

        // Final trimming of trailing empty lines
        while (!cleanedLore.isEmpty()
                && ColorUtils.stripColor(cleanedLore.get(cleanedLore.size() - 1)).trim().isEmpty()) {
            cleanedLore.remove(cleanedLore.size() - 1);
        }

        meta.setLore(cleanedLore);
    }

    private String formatStatName(String raw) {
        return plugin.getReforgeManager().formatStatName(raw);
    }

    private String formatValue(double value) {
        if (value == (long) value)
            return String.format("%d", (long) value);
        else
            return String.format("%.1f", value);
    }

}
