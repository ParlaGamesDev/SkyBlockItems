package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the ItemRarity system - loading, applying, and removing rarities from
 * items.
 */
public class RarityManager {

    public static final String NBT_RARITY_KEY = "skyblock_rarity";
    public static final String NBT_CUSTOM_KEY = "skyblock_custom_rarity";
    public static final String NBT_VERSION_KEY = "skyblock_rarity_version";
    public static final String NBT_UUID_KEY = "skyblock_item_uuid";

    private static final Rarity NONE_RARITY = new Rarity("NONE", "NONE", 0, 0, false);

    private final SkyBlockItems plugin;
    private FileConfiguration rarityConfig;
    private File rarityFile;

    private final Map<String, Rarity> rarities = new HashMap<>();
    private final Map<String, ItemMappingData> itemMappings = new HashMap<>();
    private Rarity defaultRarity = null;

    private int checkerTime = 200;
    private boolean debugMode = false;
    private List<String> loreFormat = new ArrayList<>();
    private List<String> allowedInventoryTypes = new ArrayList<>();
    private double currentConfigVersion = 0;

    /**
     * Internal data for an item mapping.
     */
    private static class ItemMappingData {
        String rarityId;

        ItemMappingData(String rarityId) {
            this.rarityId = rarityId;
        }
    }

    public RarityManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Loads or reloads the rarity configuration.
     */
    public void loadConfig() {
        rarities.clear();
        itemMappings.clear();
        defaultRarity = null;

        // Save default if not exists
        rarityFile = new File(plugin.getDataFolder(), "rarity.yml");
        if (!rarityFile.exists()) {
            plugin.saveResource("rarity.yml", false);
        }
        rarityConfig = YamlConfiguration.loadConfiguration(rarityFile);

        // Refresh version on every reload (Default fallback is 1.0)
        currentConfigVersion = 1.0;

        // Load config settings
        ConfigurationSection configSection = rarityConfig.getConfigurationSection("Config");
        if (configSection != null) {
            checkerTime = configSection.getInt("checkerTime", 200);
            debugMode = configSection.getBoolean("debug-mode", false);
            loreFormat = configSection.getStringList("lore-format");
            allowedInventoryTypes = configSection.getStringList("allowed-inventories");
            // Stable version from config, defaults to 1.0 (prevents mismatch on restarts)
            currentConfigVersion = configSection.getDouble("version", 1.0);
        }

        // Load rarities
        ConfigurationSection raritiesSection = rarityConfig.getConfigurationSection("Rarities");
        if (raritiesSection != null) {
            for (String key : raritiesSection.getKeys(false)) {
                ConfigurationSection raritySection = raritiesSection.getConfigurationSection(key);
                if (raritySection != null) {
                    String identifier = raritySection.getString("identifier", key);
                    String name = raritySection.getString("name", "&f" + identifier);
                    int weight = raritySection.getInt("weight", 1);
                    int priority = 0;
                    try {
                        priority = Integer.parseInt(key);
                    } catch (NumberFormatException e) {
                        priority = raritySection.getInt("priority", 0);
                    }
                    boolean isDefault = raritySection.getBoolean("default", false);

                    Rarity rarity = new Rarity(identifier, name, weight, priority, isDefault);
                    rarities.put(identifier.toLowerCase(), rarity);

                    if (isDefault) {
                        defaultRarity = rarity;
                    }

                    debug("Loaded rarity: " + identifier + " (weight: " + weight + ", default: " + isDefault + ")");
                }
            }
        }

        // Load item mappings
        ConfigurationSection itemsSection = rarityConfig.getConfigurationSection("Items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                String rarityId = itemsSection.isConfigurationSection(itemKey)
                        ? itemsSection.getConfigurationSection(itemKey).getString("rarity")
                        : itemsSection.getString(itemKey);

                if (rarityId != null) {
                    // SHARP: CONSISTENT NORMALIZATION (Dots to Underscores, Upper Case)
                    String normalizedKey = itemKey.replace(".", "_").toUpperCase();
                    itemMappings.put(normalizedKey, new ItemMappingData(rarityId.toLowerCase()));
                }
            }
        }

        if (debugMode) {
            plugin.getLogger()
                    .info("Loaded " + rarities.size() + " rarities and " + itemMappings.size() + " item mappings.");
        }
    }

    /**
     * Gets a rarity by its identifier (case-insensitive).
     */
    public Rarity getRarity(String identifier) {
        if (identifier == null)
            return null;
        if (identifier.equalsIgnoreCase("NONE"))
            return NONE_RARITY;
        return rarities.get(identifier.toLowerCase());
    }

    /**
     * Gets the default rarity.
     */
    public Rarity getDefaultRarity() {
        return defaultRarity;
    }

    /**
     * Gets all available rarities.
     */
    public Collection<Rarity> getAllRarities() {
        return rarities.values();
    }

    /**
     * Gets the checker task interval in ticks.
     */
    public int getCheckerTime() {
        return checkerTime;
    }

    /**
     * Determines the rarity for an item based on config rules.
     * Does not check for custom-assigned rarity (use hasCustomRarity for that).
     */
    public Rarity getRarityForItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        return getRarityForItem(item, NBTItem.get(item));
    }

    public Rarity getRarityForItem(ItemStack item, NBTItem nbtItem) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        // 1. SHARP: Check for specific UUID-locked mapping in rarity.yml
        String itemKey = getItemKey(item, nbtItem);
        // SHARP: CONSISTENT NORMALIZATION (Uppercase + _ )
        String safeKey = itemKey != null ? itemKey.replace(".", "_").toUpperCase() : null;

        if (safeKey != null) {
            ItemMappingData mappingData = itemMappings.get(safeKey);
            if (mappingData != null) {
                Rarity savedRarity = getRarity(mappingData.rarityId);
                if (savedRarity != null) {
                    return savedRarity;
                }
            }
        }

        String materialName = item.getType().name();
        ItemMeta meta = item.getItemMeta();
        // Use the passed NBTItem

        // 2. Check for manual NBT override: skyblock.rarity (Legacy/Manual)
        if (nbtItem.hasTag("skyblock.rarity")) {
            String rarityId = nbtItem.getString("skyblock.rarity");
            Rarity nbtRarity = getRarity(rarityId);
            if (nbtRarity != null) {
                return nbtRarity;
            }
        }

        // 2. Check MMOItems specific mapping
        if (nbtItem.hasTag("MMOITEMS_ITEM_ID")) {
            String type = nbtItem.getString("MMOITEMS_ITEM_TYPE");
            String id = nbtItem.getString("MMOITEMS_ITEM_ID");
            String mmoKey = "MMOITEM:" + type + ":" + id;

            ItemMappingData mapping = itemMappings.get(mmoKey.toUpperCase());
            if (mapping != null) {
                if (mapping.rarityId.equalsIgnoreCase("NONE"))
                    return NONE_RARITY;
                return getRarity(mapping.rarityId);
            }
            // MMOItems NEVER fall through to material checks
            return defaultRarity;
        }

        // 3. Check CustomModelData format: MATERIAL:CMD
        if (meta != null && meta.hasCustomModelData()) {
            String cmdKey = materialName + ":" + meta.getCustomModelData();
            ItemMappingData mapping = itemMappings.get(cmdKey.toUpperCase());
            if (mapping != null) {
                if (mapping.rarityId.equalsIgnoreCase("NONE"))
                    return NONE_RARITY;
                return getRarity(mapping.rarityId);
            }
            // CMD items NEVER fall through to material checks
            return defaultRarity;
        }

        // 4. Check Enchanted Book format: ENCHANTED_BOOK:ENCHANT:LEVEL
        if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta esm) {
            for (Map.Entry<Enchantment, Integer> entry : esm.getStoredEnchants().entrySet()) {
                String bookKey = "ENCHANTED_BOOK:" + entry.getKey().getKey().getKey().toUpperCase() + ":"
                        + entry.getValue();
                ItemMappingData mapping = itemMappings.get(bookKey.toUpperCase());
                if (mapping != null) {
                    if (mapping.rarityId.equalsIgnoreCase("NONE"))
                        return NONE_RARITY;
                    return getRarity(mapping.rarityId);
                }
            }
            // Books NEVER fall through to generic material checks
            return defaultRarity;
        }

        // 5. Check Potion format: POTION:EFFECT:EXTENDED:UPGRADED
        if ((item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION ||
                item.getType() == Material.LINGERING_POTION) && meta instanceof PotionMeta pm) {
            PotionType type = pm.getBasePotionType();
            if (type != null) {
                String typeName = type.name();
                boolean extended = typeName.contains("EXTENDED") || typeName.contains("LONG");
                boolean upgraded = typeName.contains("STRONG") || typeName.contains("II");
                String potionKey = item.getType().name() + ":" + typeName + ":" + extended + ":" + upgraded;
                ItemMappingData mapping = itemMappings.get(potionKey.toUpperCase());
                if (mapping != null) {
                    if (mapping.rarityId.equalsIgnoreCase("NONE"))
                        return NONE_RARITY;
                    return getRarity(mapping.rarityId);
                }
            }
            // Potions NEVER fall through to generic material checks
            return defaultRarity;
        }

        // 6. Check plain material (Only reached for pure Vanilla items)
        ItemMappingData mapping = itemMappings.get(materialName.toUpperCase());
        if (mapping != null) {
            if (mapping.rarityId.equalsIgnoreCase("NONE"))
                return NONE_RARITY;
            return getRarity(mapping.rarityId);
        }

        // Return default rarity
        return defaultRarity;
    }

    /**
     * Gets the current rarity of an item from NBT.
     */
    public Rarity getCurrentRarity(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        return getCurrentRarity(NBTItem.get(item));
    }

    public Rarity getCurrentRarity(NBTItem nbtItem) {
        if (nbtItem == null) {
            return null;
        }
        if (nbtItem.hasTag(NBT_RARITY_KEY)) {
            String rarityId = nbtItem.getString(NBT_RARITY_KEY);
            if (rarityId != null && rarityId.equalsIgnoreCase("NONE")) {
                return NONE_RARITY;
            }
            return getRarity(rarityId);
        }
        return null;
    }

    /**
     * Checks if an item has a custom (command-assigned) rarity.
     */
    public boolean hasCustomRarity(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return hasCustomRarity(NBTItem.get(item));
    }

    public boolean hasCustomRarity(NBTItem nbtItem) {
        if (nbtItem == null) {
            return false;
        }
        return nbtItem.hasTag(NBT_CUSTOM_KEY) && nbtItem.getBoolean(NBT_CUSTOM_KEY);
    }

    /**
     * Applies a rarity to an item, updating NBT, name, and lore.
     *
     * @param item     The item to modify
     * @param rarity   The rarity to apply
     * @param isCustom True if this is a command-assigned rarity (takes priority)
     * @return The modified item
     */
    public ItemStack applyRarity(ItemStack item, Rarity rarity, boolean isCustom) {
        if (item == null || item.getType() == Material.AIR || rarity == null) {
            return item;
        }

        // Apply NBT using ItemTag
        NBTItem nbtItem = NBTItem.get(item);
        nbtItem.addTag(new ItemTag(NBT_RARITY_KEY, rarity.getIdentifier()));
        nbtItem.addTag(new ItemTag(NBT_CUSTOM_KEY, isCustom));
        nbtItem.addTag(new ItemTag(NBT_VERSION_KEY, currentConfigVersion));

        // SHARP: If this is a custom assignment, ensure the item has a unique ID
        // so it can be persisted in rarity.yml without affecting all items of the same
        // material.
        if (isCustom && !nbtItem.hasTag("mmoitems_uuid") && !nbtItem.hasTag(NBT_UUID_KEY)) {
            nbtItem.addTag(new ItemTag(NBT_UUID_KEY, UUID.randomUUID().toString()));
        }

        ItemStack result = nbtItem.toItem();

        // Update name and lore (using original item lore)
        result = updateRarityLore(result, rarity);

        // CRITICAL FIX: Save to rarity.yml if this is a custom rarity assignment
        if (isCustom) {
            saveMapping(result, rarity.getIdentifier(), nbtItem);
        }

        debug("Applied rarity " + rarity.getIdentifier() + " to " + item.getType() + " (custom: " + isCustom + ")");
        return result;
    }

    /**
     * Removes rarity from an item (NBT and lore).
     * Sets rarity to "NONE".
     *
     * @param isCustom If true, marks as custom (prevent auto-assign). If false,
     *                 it's just cleanup.
     */
    public ItemStack removeRarity(ItemStack item, boolean isCustom) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        // Apply NBT marks
        NBTItem nbtItem = NBTItem.get(item);

        // If it's a manual removal, we mark it as NONE so getRarityForItem sees the
        // override
        if (isCustom) {
            nbtItem.addTag(new ItemTag(NBT_RARITY_KEY, "NONE"));
            nbtItem.addTag(new ItemTag(NBT_CUSTOM_KEY, true));
            // CRITICAL: Clean up YAML when manually removed
            removeMapping(item);
        } else {
            // If it's just cleanup, wipe the tags entirely
            nbtItem.removeTag(NBT_RARITY_KEY);
            nbtItem.removeTag(NBT_CUSTOM_KEY);
        }

        // Always stamp the current version to prevent redundant re-processing
        nbtItem.addTag(new ItemTag(NBT_VERSION_KEY, currentConfigVersion));

        item = nbtItem.toItem();

        // Remove rarity lore
        item = removeRarityLore(item);

        debug("Permanently removed rarity from " + item.getType() + " (isCustom: " + isCustom + ")");
        return item;
    }

    /**
     * Processes an item - applies rarity if needed.
     * Respects custom rarity priority.
     *
     * @return The processed item (may be unchanged)
     */
    public ItemStack processItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return item;
        }

        // EXCLUSION: Reforge Gems should NEVER have rarity applied
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey gemKey = new NamespacedKey(plugin, "reforge_gem_id");
            if (meta.getPersistentDataContainer().has(gemKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                // If it's a gem, ensure it has NO rarity and skip processing
                if (hasRarityLore(item)) {
                    return removeRarity(item, false);
                }
                return item;
            }
        }

        NBTItem nbt = NBTItem.get(item);

        // 0. SHARP PERSISTENCE: Check if item has NBT-assigned custom rarity that needs
        // saving
        // We ALWAYS check for sync because the version check only applies to Lore/NBT
        // application logic
        boolean synced = checkAndSaveCustomRarity(item, nbt);

        // Check if item already has rarity
        Rarity currentRarity = getCurrentRarity(nbt);
        Rarity targetRarity = getRarityForItem(item, nbt);

        // 1. SHARP: Handle EXPLICIT NONE (Manual Removal / Override)
        if (targetRarity != null && targetRarity.getIdentifier().equalsIgnoreCase("NONE")) {
            // Check if we need to strip lore or NBT (is it currently displaying something?)
            if (currentRarity == null || !currentRarity.getIdentifier().equalsIgnoreCase("NONE")
                    || hasRarityLore(item) || synced) {
                return removeRarity(item, false);
            }

            // Even if already NONE, check version for NBT stamp consistency
            if (!nbt.hasTag(NBT_VERSION_KEY) || nbt.getDouble(NBT_VERSION_KEY) != currentConfigVersion) {
                return removeRarity(item, false);
            }

            return item;
        }

        // 2. Skip if already has custom rarity (unless target was NONE, handled above)
        if (hasCustomRarity(nbt)) {
            // Still check version - if version mismatch OR if we just synced from NBT
            // re-apply the custom rarity to refresh lore
            if (nbt.hasTag(NBT_VERSION_KEY) && nbt.getDouble(NBT_VERSION_KEY) == currentConfigVersion && !synced) {
                return item;
            }
            // Re-apply to refresh lore/NBT format if version changed or synced
            if (currentRarity != null) {
                return applyRarity(item, currentRarity, true);
            }
        }

        // If no target rarity (not in config and no default), skip
        if (targetRarity == null) {
            return item;
        }

        // If already has the correct rarity, check if design version matches
        if (currentRarity != null && currentRarity.getIdentifier().equalsIgnoreCase(targetRarity.getIdentifier())) {
            double itemVersion = nbt.hasTag(NBT_VERSION_KEY) ? nbt.getDouble(NBT_VERSION_KEY) : -1.0;

            // If and ONLY if the versions match exactly AND we didn't just sync, we skip.
            if (itemVersion == currentConfigVersion && !synced) {
                // IMPORTANT: Return original item instance to avoid triggering setItem
                return item;
            }
        }

        // Apply the rarity
        ItemStack result = applyRarity(item, targetRarity, false);
        
        // Final sanity check: if the item barely changed (e.g. meta equals), we might still want to return original
        // but applyRarity already creates a new item via nbtItem.toItem(), so we return the result.
        return result;
    }

    /**
     * Checks if the item has un-persisted custom rarity NBT and saves it if needed.
     * This fixes the issue where custom NBT tags are lost on restart because they
     * weren't in rarity.yml.
     */
    private boolean checkAndSaveCustomRarity(ItemStack item, NBTItem nbt) {
        if (item == null || nbt == null)
            return false;

        // SHARP SYNC LOGIC:
        // 1. If item version is STALE, it means YAML was just reloaded/edited.
        // In this case, YAML is the source of truth. DO NOT sync NBT logic back to
        // YAML.
        double itemVersion = nbt.hasTag(NBT_VERSION_KEY) ? nbt.getDouble(NBT_VERSION_KEY) : -1.0;
        if (itemVersion != -1.0 && itemVersion != currentConfigVersion) {
            return false;
        }

        if (nbt.hasTag(NBT_RARITY_KEY)) {
            String rarityId = nbt.getString(NBT_RARITY_KEY);
            String key = getItemKey(item, nbt);
            if (key != null) {
                // SANITIZE KEY (A.B -> A_B)
                String safeKey = key.replace(".", "_").toUpperCase();

                ItemMappingData cached = itemMappings.get(safeKey);

                // Check if we need to sync
                boolean needsSync = false;
                if (cached == null) {
                    // Not in config, but has NBT rarity -> Sync if not NONE
                    if (rarityId != null && !rarityId.equalsIgnoreCase("NONE")) {
                        needsSync = true;
                    }
                } else if (rarityId != null && !cached.rarityId.equalsIgnoreCase(rarityId)) {
                    // In config, but value differs -> Sync
                    needsSync = true;
                }

                if (needsSync) {
                    // Auto-assign isCustom=true because it comes from NBT
                    saveMapping(item, rarityId, false, nbt);
                    return true;
                }
            }
        } else if (nbt.hasTag(NBT_CUSTOM_KEY) && nbt.getBoolean(NBT_CUSTOM_KEY)) {
            // Has custom marker but NO rarity tag -> This could be a manual removal via
            // command.
            String key = getItemKey(item, nbt);
            if (key != null) {
                String safeKey = key.replace(".", "_").toUpperCase();
                if (itemMappings.containsKey(safeKey)) {
                    // In cache but NBT says NONE -> Removal Sync
                    saveMapping(item, "NONE", false, nbt);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the item lore contains any rarity-related lines.
     */
    private boolean hasRarityLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore())
            return false;
        List<String> lore = meta.getLore();
        for (String line : lore) {
            String stripped = stripFullColor(line).toLowerCase().trim();
            if (stripped.contains("rarity:") ||
                    stripped.contains("נדירות:") ||
                    stripped.contains("item-rarity") ||
                    isRarityName(stripped)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the item's lore with rarity information.
     */
    public ItemStack updateRarityLore(ItemStack item, Rarity rarity) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // Ensure display name is colorized (Fixes &e&l showing as raw text)
        if (meta.hasDisplayName()) {
            meta.setDisplayName(ColorUtils.colorize(meta.getDisplayName()));
        }

        // Get current lore, remove any existing rarity lines
        List<String> currentLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        currentLore = stripRarityLore(currentLore);

        // Build new lore from format
        List<String> newLore = new ArrayList<>();
        String coloredRarity = ColorUtils.colorize(rarity.getDisplayName());

        for (String formatLine : loreFormat) {
            if (formatLine.contains("{item-lore}")) {
                // Insert original lore here
                for (String originalLine : currentLore) {
                    newLore.add(originalLine);
                }

                // Enforce a gap if lore exists and format doesn't provide one
                if (!currentLore.isEmpty()) {
                    boolean nextIsAlreadyBlank = false;
                    int currentIndex = -1;
                    for (int i = 0; i < loreFormat.size(); i++) {
                        if (loreFormat.get(i).contains("{item-lore}")) {
                            currentIndex = i;
                            break;
                        }
                    }
                    if (currentIndex != -1 && currentIndex + 1 < loreFormat.size()) {
                        if (stripFullColor(loreFormat.get(currentIndex + 1)).trim().isEmpty()) {
                            nextIsAlreadyBlank = true;
                        }
                    }

                    if (!nextIsAlreadyBlank) {
                        newLore.add("");
                    }
                }
            } else {
                String processedLine = formatLine.replace("{rarity-prefix}", coloredRarity);
                newLore.add(ColorUtils.colorize(processedLine));
            }
        }

        // Final reconstruction with safety gap
        List<String> finalLore = new ArrayList<>();
        boolean inGap = false;
        for (int i = 0; i < newLore.size(); i++) {
            String line = newLore.get(i);
            boolean isBlank = stripFullColor(line).trim().isEmpty();

            // If it's a blank line and we're nearing the rarity prefix...
            if (isBlank) {
                if (!inGap) {
                    finalLore.add(line);
                    inGap = true;
                }
            } else {
                finalLore.add(line);
                inGap = false;
            }
        }

        meta.setLore(finalLore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Removes rarity-specific lore lines.
     */
    private ItemStack removeRarityLore(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        // Reset name color if needed (optional but good for consistency)
        if (meta.hasDisplayName()) {
            meta.setDisplayName(ColorUtils.colorize(meta.getDisplayName()));
        }

        if (meta.hasLore()) {
            List<String> currentLore = new ArrayList<>(meta.getLore());
            currentLore = stripRarityLore(currentLore);
            meta.setLore(currentLore);
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Strips rarity-related lines from lore.
     */
    public List<String> stripRarityLore(List<String> lore) {
        List<String> result = new ArrayList<>();

        for (String line : lore) {
            String stripped = stripFullColor(line).toLowerCase().trim();
            // Check for various rarity indicators
            if (stripped.contains("rarity:") ||
                    stripped.contains("נדירות:") ||
                    stripped.contains("item-rarity") ||
                    isRarityName(stripped)) {
                continue;
            }
            result.add(line);
        }

        // Always clean trailing empty lines to ensure the gap we add later is clean
        while (!result.isEmpty() && stripFullColor(result.get(result.size() - 1)).trim().isEmpty()) {
            result.remove(result.size() - 1);
        }

        return result;
    }

    private String stripFullColor(String text) {
        if (text == null)
            return "";

        // 1. Strip ChatColor § codes (including hex §x§r§r§g§g§b§b)
        // We use a manual loop to be more reliable than stripColor for all hex
        // variations
        StringBuilder sb = new StringBuilder();
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '§' || chars[i] == '&') {
                if (i + 1 < chars.length) {
                    char next = chars[i + 1];
                    // Skip next char if it's a format code
                    if ("0123456789abcdefklmnorx#".indexOf(Character.toLowerCase(next)) != -1) {
                        i++;
                        // If it's a hex start, skip more
                        if (next == '#' || Character.toLowerCase(next) == 'x') {
                            // Try to skip next 6 chars if hex
                            int skip = 0;
                            while (skip < 6 && i + 1 < chars.length) {
                                char hex = chars[i + 1];
                                if ("0123456789abcdef§".indexOf(Character.toLowerCase(hex)) != -1) {
                                    i++;
                                    skip++;
                                } else {
                                    break;
                                }
                            }
                        }
                        continue;
                    }
                }
            }
            sb.append(chars[i]);
        }
        String stripped = sb.toString();

        // 2. Final regex safety strip for any missed tags like <#RRGGBB>
        return stripped
                .replaceAll("(?i)<#[0-9a-f]{6}>", "")
                .replaceAll("(?i)&#[0-9a-f]{6}", "")
                .trim();
    }

    private boolean isRarityName(String text) {
        String strippedText = text.toLowerCase().trim();
        if (strippedText.equals("none"))
            return true;
        for (Rarity r : rarities.values()) {
            String id = r.getIdentifier().toLowerCase().trim();
            String name = stripFullColor(r.getDisplayName()).toLowerCase().trim();
            if (strippedText.equals(id) || strippedText.equals(name))
                return true;
        }
        return false;
    }

    public void saveMapping(ItemStack item, String rarityId) {
        saveMapping(item, rarityId, NBTItem.get(item));
    }

    public void saveMapping(ItemStack item, String rarityId, NBTItem nbt) {
        saveMapping(item, rarityId, true, nbt);
    }

    public void saveMapping(ItemStack item, String rarityId, boolean refresh, NBTItem nbt) {
        String key = getItemKey(item, nbt);
        if (key == null)
            return;

        // Use a safe key for YAML paths (escaping dots) - SHARP: CONSISTENT
        // ATOMIC PERSISTENCE: Reload from disk immediately before modification
        // This prevents overwriting manual user edits made while the plugin was
        // running.
        rarityConfig = YamlConfiguration.loadConfiguration(rarityFile);

        // Normalize the key for YAML and cache
        String safeKey = key.replace(".", "_").toUpperCase();
        String safePath = "Items." + safeKey;

        // Logic check: Is this a removal?
        boolean isNone = rarityId == null || rarityId.equalsIgnoreCase("NONE");

        if (isNone) {
            // Check if there is a DEFAULT rarity for this item type
            Rarity defaultRarityForType = getRarityForItem(new ItemStack(item.getType()));

            if (defaultRarityForType != null && !defaultRarityForType.getIdentifier().equalsIgnoreCase("NONE")) {
                // If there IS a default, we MUST save 'NONE' to override it
                rarityConfig.set(safePath + ".rarity", "NONE");
                saveAndReload(safeKey, "NONE", refresh);
            } else {
                // If there is NO default, we can just delete the entire record
                rarityConfig.set(safePath, null);
                saveAndReload(safeKey, "REMOVED", refresh);
            }
        } else {
            // Save Custom Rarity (using the long format for consistency)
            rarityConfig.set(safePath + ".rarity", rarityId);
            saveAndReload(safeKey, rarityId, refresh);
        }
    }

    private void saveAndReload(String key, String value, boolean refresh) {
        try {
            rarityConfig.save(rarityFile);

            // Optimization: Update in-memory map directly instead of full reload
            // key passed here is ALREADY sanitized (A_B)
            String upperKey = key.toUpperCase();
            if (value.startsWith("REMOVED")) {
                itemMappings.remove(upperKey);
            } else {
                itemMappings.put(upperKey, new ItemMappingData(value.toLowerCase()));
            }

            if (refresh) {
                for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    refreshPlayer(player);
                }
            }
            if (debugMode)
                plugin.getLogger().info("Updated rarity mapping: " + key + " -> " + value);
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not save rarity.yml: " + e.getMessage());
        }
    }

    /**
     * Efficiently processes a player's inventory, minimizing packet updates.
     */
    public void processInventory(org.bukkit.entity.Player player) {
        if (player == null || !player.isOnline())
            return;

        // Note: Removed early return for cursor items to ensure deduplication 
        // works immediately after Creative Pick Block / Drag actions.

        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                ItemStack processed = processItem(item);
                if (processed != item) { // Direct reference comparison works due to processItem logic
                    inv.setItem(i, processed);
                    changed = true;
                }
            }
        }

        if (changed) {
            debug("Updated inventory for " + player.getName() + " (minimized packets)");
        }
    }

    public void removeMapping(ItemStack item) {
        saveMapping(item, "NONE", true, NBTItem.get(item)); // This triggers the deletion logic in saveMapping
    }

    /**
     * Generates a unique config key for an ItemStack.
     * SHARP: Prioritizes UUID for persistence.
     */
    public String getItemKey(ItemStack item) {
        return getItemKey(item, false);
    }

    public String getItemKey(ItemStack item, boolean ignoreUUID) {
        if (item == null || item.getType() == Material.AIR)
            return null;
        return getItemKey(item, nbtItemGet(item), ignoreUUID);
    }

    public String getBaseKey(ItemStack item) {
        return getItemKey(item, true);
    }

    private NBTItem nbtItemGet(ItemStack item) {
        try {
            return NBTItem.get(item);
        } catch (Exception e) {
            return null;
        }
    }

    public String getItemKey(ItemStack item, NBTItem nbtItem) {
        return getItemKey(item, nbtItem, false);
    }

    public String getItemKey(ItemStack item, NBTItem nbtItem, boolean ignoreUUID) {
        if (item == null || item.getType() == Material.AIR || nbtItem == null)
            return null;

        // 0. SHARP: Plugin-specific UUID (Manual assignments for Vanilla items)
        if (!ignoreUUID && nbtItem.hasTag(NBT_UUID_KEY)) {
            return "UUID_" + nbtItem.getString(NBT_UUID_KEY);
        }

        // 1. MMOItems UUID (Most stable for tracking)
        if (!ignoreUUID && nbtItem.hasTag("mmoitems_uuid")) {
            return "UUID_" + nbtItem.getString("mmoitems_uuid");
        }

        // 2. MMOItems Type/ID Fallback
        if (nbtItem.hasTag("MMOITEMS_ITEM_ID")) {
            return "MMOITEM:" + nbtItem.getString("MMOITEMS_ITEM_TYPE") + ":" + nbtItem.getString("MMOITEMS_ITEM_ID");
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasCustomModelData()) {
            return item.getType().name() + ":" + meta.getCustomModelData();
        }

        if (item.getType() == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta esm) {
            Map.Entry<Enchantment, Integer> entry = esm.getStoredEnchants().entrySet().stream().findFirst()
                    .orElse(null);
            if (entry != null) {
                return "ENCHANTED_BOOK:" + entry.getKey().getKey().getKey().toUpperCase() + ":" + entry.getValue();
            }
        }

        if ((item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION ||
                item.getType() == Material.LINGERING_POTION) && meta instanceof PotionMeta pm) {
            PotionType type = pm.getBasePotionType();
            if (type != null) {
                String typeName = type.name();
                boolean extended = typeName.contains("EXTENDED") || typeName.contains("LONG");
                boolean upgraded = typeName.contains("STRONG") || typeName.contains("II");
                return item.getType().name() + ":" + typeName + ":" + extended + ":" + upgraded;
            }
        }

        // 3. Material Fallback
        return item.getType().name();
    }

    /**
     * Forces a re-processing of all items in a player's inventory.
     */
    private void refreshPlayer(org.bukkit.entity.Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                // SHARP REFRESH: Strip all previous rarity markers to force a clean evaluate
                // against the new config truth.
                NBTItem nbtItem = NBTItem.get(item);
                nbtItem.removeTag(NBT_RARITY_KEY);
                nbtItem.removeTag(NBT_CUSTOM_KEY);
                nbtItem.removeTag(NBT_VERSION_KEY);
                item = nbtItem.toItem();

                ItemStack processed = processItem(item);
                if (processed != item) {
                    player.getInventory().setItem(i, processed);
                    changed = true;
                }
            }
        }
        if (changed) {
            player.updateInventory();
        }
    }

    /**
     * Checks if an inventory is allowed to have rarities processed in it.
     */
    public boolean isAllowedInventory(org.bukkit.inventory.InventoryView view) {
        if (view == null)
            return false;

        org.bukkit.inventory.Inventory top = view.getTopInventory();
        if (top == null)
            return true; // Just player inventory usually

        // Player inventory is ALWAYS allowed
        if (top.getType() == org.bukkit.event.inventory.InventoryType.PLAYER)
            return true;

        // Check if type is allowed (Whitelist)
        String typeName = top.getType().name();
        if (!allowedInventoryTypes.contains(typeName)) {
            return false;
        }

        // Special handling for common GUI types (CHEST, DISPENSER, etc.)
        // We want to ensure they are physical containers, not virtual GUIs.
        // Physical blocks implement BlockInventoryHolder.
        if (top.getType() == org.bukkit.event.inventory.InventoryType.CHEST ||
                top.getType() == org.bukkit.event.inventory.InventoryType.BARREL ||
                top.getType() == org.bukkit.event.inventory.InventoryType.SHULKER_BOX ||
                top.getType() == org.bukkit.event.inventory.InventoryType.HOPPER ||
                top.getType() == org.bukkit.event.inventory.InventoryType.DISPENSER ||
                top.getType() == org.bukkit.event.inventory.InventoryType.DROPPER) {

            // If it's one of these types but has no block holder, it's likely a virtual GUI
            if (!(top.getHolder() instanceof org.bukkit.inventory.BlockInventoryHolder)) {
                debug("Inventory type " + typeName
                        + " is whitelisted but has no physical block holder. Ignoring (GUI).");
                return false;
            }
        }

        return true;
    }

    /**
     * Logs a debug message if debug mode is enabled.
     */
    public void debug(String message) {
        if (debugMode) {
            plugin.getLogger().log(Level.INFO, "[Rarity Debug] " + message);
        }
    }

    public static String getNbtRarityKey() {
        return NBT_RARITY_KEY;
    }

    public static String getNbtCustomKey() {
        return NBT_CUSTOM_KEY;
    }
}
