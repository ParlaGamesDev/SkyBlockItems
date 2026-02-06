package dev.agam.skyblockitems.enchantsystem.managers;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Level;

/**
 * Manages enchantment definitions loaded from enchants.yml.
 */
public class EnchantManager {

    private final SkyBlockItems plugin;
    private final Map<String, EnchantConfig> enchants = new LinkedHashMap<>();

    public EnchantManager(SkyBlockItems plugin) {
        this.plugin = plugin;
        loadEnchants();
    }

    public void loadEnchants() {
        enchants.clear();

        FileConfiguration config = plugin.getConfigManager().getEnchantsConfig();
        ConfigurationSection enchantsSection = config.getConfigurationSection("enchants");

        if (enchantsSection == null) {
            plugin.getLogger().warning("No enchants section found in enchants.yml!");
            return;
        }

        for (String key : enchantsSection.getKeys(false)) {
            try {
                loadEnchant(key, enchantsSection.getConfigurationSection(key));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load enchant: " + key, e);
            }
        }

        plugin.getLogger().info("Loaded " + enchants.size() + " enchantments.");
    }

    private void loadEnchant(String id, ConfigurationSection section) {
        if (section == null)
            return;

        String displayName = section.getString("display-name", id);
        String description = section.getString("description", "");
        String materialStr = section.getString("material", "ENCHANTED_BOOK");
        Material material = Material.getMaterial(materialStr);
        if (material == null)
            material = Material.ENCHANTED_BOOK;

        List<String> targets = section.getStringList("targets");
        if (targets.isEmpty())
            targets.add("GLOBAL");

        String vanillaEnchant = section.getString("vanilla-enchant");
        String mmoitemsStat = section.getString("mmoitems-stat");
        String auraskillsStat = section.getString("auraskills-stat");

        List<String> conflicts = section.getStringList("conflicts");

        // Required AuraSkills enchanting level to unlock this enchant - read only from
        // enchants.yml
        int requiredEnchantingLevel = section.getInt("required-enchanting-level", 0);

        int anvilMultiplier = section.getInt("anvil-multiplier", 2);

        String attributeName = section.getString("attribute");
        String attributeSlot = section.getString("attribute-slot", "HAND");

        double cooldown = section.getDouble("cooldown", 0);
        boolean enabled = section.getBoolean("enabled", true);

        // Load levels
        Map<Integer, LevelConfig> levels = new HashMap<>();
        ConfigurationSection levelsSection = section.getConfigurationSection("levels");

        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                int level = Integer.parseInt(levelKey);
                ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                if (levelSection != null) {
                    String value = levelSection.getString("value", "0");
                    int xpCost = levelSection.getInt("xp-cost", 1);
                    levels.put(level, new LevelConfig(value, xpCost));
                }
            }
        } else {
            // Single level enchant
            String value = section.getString("value", "1");
            int xpCost = section.getInt("xp-cost", 1);
            levels.put(1, new LevelConfig(value, xpCost));
        }

        enchants.put(id, new EnchantConfig(
                id, displayName, description, material, targets,
                vanillaEnchant, mmoitemsStat, auraskillsStat,
                attributeName, attributeSlot, cooldown, enabled, levels, conflicts, requiredEnchantingLevel,
                anvilMultiplier));
    }

    public Map<String, EnchantConfig> getEnchants() {
        return enchants;
    }

    public EnchantConfig getEnchant(String id) {
        return enchants.get(id.toLowerCase());
    }

    /**
     * Checks if an enchantment conflicts with any existing enchantments on an item.
     * 
     * @return The display name of the conflicting enchantment, or null if no
     *         conflict.
     */
    public String getConflict(ItemStack item, EnchantConfig enchant) {
        if (item == null || !item.hasItemMeta())
            return null;
        List<String> conflicts = enchant.getConflicts();
        if (conflicts == null || conflicts.isEmpty())
            return null;

        // Check vanilla enchants on item
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            String vanillaId = entry.getKey().getKey().getKey().toLowerCase();
            // Check if this vanilla
            if (conflicts.contains(vanillaId)) {
                return getDisplayNameForId(vanillaId);
            }
        }

        // Check for custom enchants and our custom-defined vanilla enchants in lore
        if (item.getItemMeta().hasLore()) {
            List<String> lore = item.getItemMeta().getLore();
            for (String line : lore) {
                String strippedLine = ChatColor.stripColor(line);
                for (String conflictId : conflicts) {
                    String conflictName = getDisplayNameForId(conflictId);
                    String strippedConflictName = ChatColor.stripColor(ColorUtils.colorize(conflictName));

                    if (strippedLine.startsWith(strippedConflictName)) {
                        return conflictName;
                    }
                }
            }
        }

        return null;
    }

    public Map<String, Integer> parseLore(List<String> lore) {
        Map<String, Integer> result = new HashMap<>();
        if (lore == null)
            return result;

        Map<String, String> nameToId = new HashMap<>();
        enchants.forEach((id, c) -> nameToId
                .put(ChatColor.stripColor(ColorUtils.colorize(c.getDisplayName())).toLowerCase(), id.toLowerCase()));

        for (String line : lore) {
            String clean = ChatColor.stripColor(line).toLowerCase();
            // Split by comma in case multiple enchants are on one line
            String[] parts = clean.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                for (Map.Entry<String, String> entry : nameToId.entrySet()) {
                    String name = entry.getKey();
                    if (trimmed.startsWith(name)) {
                        String levelPart = trimmed.substring(name.length()).trim();
                        int lvl = fromRomanInternal(levelPart);
                        if (lvl > 0) {
                            result.put(entry.getValue(), lvl);
                            break;
                        } else if (levelPart.isEmpty()) {
                            // Single level enchant or name matches exactly
                            result.put(entry.getValue(), 1);
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    public String getDisplayNameForId(String id) {
        EnchantConfig conf = getEnchant(id);
        if (conf != null)
            return conf.getDisplayName();

        CustomEnchant custom = plugin.getCustomEnchantManager().getEnchant(id);
        if (custom != null)
            return custom.getDisplayName();

        return id;
    }

    public ItemStack createEnchantedBook(CustomEnchant enchant, int level) {
        return createBook(enchant.getDisplayName(), level, null);
    }

    public ItemStack createEnchantedBook(EnchantConfig enchant, int level) {
        return createBook(enchant.getDisplayName(), level, enchant.getVanillaEnchant());
    }

    public void applyEnchantment(ItemStack item, String id, int level) {
        EnchantConfig config = getEnchant(id);
        if (config == null)
            return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        // 1. Apply Vanilla Enchant
        if (config.getVanillaEnchant() != null) {
            Enchantment v = Enchantment.getByKey(NamespacedKey.minecraft(config.getVanillaEnchant().toLowerCase()));
            if (v != null) {
                if (meta instanceof EnchantmentStorageMeta esm) {
                    esm.addStoredEnchant(v, level, true);
                } else {
                    meta.addEnchant(v, level, true);
                }
            }
        }

        // 2. Apply/Update Attribute Modifier
        if (config.getAttributeName() != null) {
            try {
                // Correct lookup for Spigot 1.21+: map GENERIC_MAX_HEALTH -> generic.max_health
                String attrKey = config.getAttributeName().toLowerCase();
                if (attrKey.startsWith("generic_"))
                    attrKey = attrKey.replaceFirst("generic_", "generic.");
                if (attrKey.equals("max_health"))
                    attrKey = "generic.max_health";
                if (attrKey.equals("attack_damage"))
                    attrKey = "generic.attack_damage";
                if (attrKey.equals("movement_speed"))
                    attrKey = "generic.movement_speed";
                if (attrKey.equals("armor"))
                    attrKey = "generic.armor";
                if (attrKey.equals("luck"))
                    attrKey = "generic.luck";

                Attribute attribute = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrKey));
                if (attribute == null) {
                    plugin.getLogger()
                            .warning("Attribute " + config.getAttributeName() + " (" + attrKey + ") not found!");
                    return;
                }
                EquipmentSlot slot = EquipmentSlot.valueOf(config.getAttributeSlot().toUpperCase());

                // Clear existing modifiers for this specific enchant using NamespacedKey
                NamespacedKey nkey = new NamespacedKey(plugin, "enchant_" + id.toLowerCase());
                Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
                if (modifiers != null) {
                    modifiers.stream()
                            .filter(mod -> mod.getKey().equals(nkey))
                            .forEach(mod -> meta.removeAttributeModifier(attribute, mod));
                }

                LevelConfig lvlConf = config.getLevel(level);
                if (lvlConf != null) {
                    // Spigot 1.21+ constructor
                    AttributeModifier modifier = new AttributeModifier(nkey, lvlConf.getDoubleValue(),
                            AttributeModifier.Operation.ADD_NUMBER, getSlotGroup(slot));
                    meta.addAttributeModifier(attribute, modifier);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to apply attribute " + config.getAttributeName() + " for " + id);
            }
        }

        // 3. Cosmetic Glint for non-vanilla
        if (config.getVanillaEnchant() == null) {
            meta.setEnchantmentGlintOverride(true);
        }

        // 4. Update Lore
        Map<String, Integer> currentEnchants = parseLore(meta.getLore());
        currentEnchants.put(id.toLowerCase(), level);
        meta.setLore(rebuildLore(meta.getLore(), currentEnchants));

        // 5. Flags
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
    }

    public List<String> rebuildLore(List<String> original, Map<String, Integer> enchants) {
        List<String> nonEnchantLines = new ArrayList<>();
        if (original != null) {
            Set<String> enchantNames = new HashSet<>();
            this.enchants.values().forEach(
                    e -> enchantNames.add(ChatColor.stripColor(ColorUtils.colorize(e.getDisplayName())).toLowerCase()));

            for (String line : original) {
                String clean = ChatColor.stripColor(line).toLowerCase().trim();
                boolean isEnchant = false;
                for (String name : enchantNames) {
                    if (clean.contains(name)) {
                        isEnchant = true;
                        break;
                    }
                }
                if (!isEnchant && !clean.isEmpty())
                    nonEnchantLines.add(line);
            }
        }

        List<String> newLore = new ArrayList<>();
        List<String> entries = new ArrayList<>();
        List<String> sortedIds = new ArrayList<>(enchants.keySet());
        Collections.sort(sortedIds);

        for (String id : sortedIds) {
            EnchantConfig conf = getEnchant(id);
            if (conf == null)
                continue;
            String roman = toRoman(enchants.get(id));
            entries.add(ChatColor.GRAY + ChatColor.stripColor(ColorUtils.colorize(conf.getDisplayName()))
                    + (roman.isEmpty() ? "" : " " + roman));
        }

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0 && i % 3 != 0)
                line.append("§7, ");
            line.append(entries.get(i));
            if ((i + 1) % 3 == 0 || i == entries.size() - 1) {
                newLore.add(line.toString());
                line = new StringBuilder();
            }
        }
        newLore.addAll(nonEnchantLines);
        return newLore;
    }

    private EquipmentSlotGroup getSlotGroup(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            case HAND -> EquipmentSlotGroup.MAINHAND;
            case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
            default -> EquipmentSlotGroup.ANY;
        };
    }

    private int fromRomanInternal(String r) {
        if (r == null || r.isEmpty())
            return 0;
        return switch (r.toUpperCase()) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            default -> 0;
        };
    }

    private ItemStack createBook(String displayName, int level, String vanillaId) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

        if (meta != null) {
            boolean hasVanillaEnchant = false;

            // 1. Try Real Vanilla Enchant if defined
            if (vanillaId != null && !vanillaId.isEmpty()) {
                Enchantment v = Enchantment.getByKey(NamespacedKey.minecraft(vanillaId.toLowerCase()));
                if (v != null) {
                    meta.addStoredEnchant(v, level, true);
                    hasVanillaEnchant = true;
                    // Vanilla enchants display natively on books - no custom lore needed
                }
            }

            // 2. For custom/non-vanilla enchants: add custom lore + cosmetic glow
            if (!hasVanillaEnchant) {
                String roman = toRoman(level);
                String name = ChatColor.stripColor(ColorUtils.colorize(displayName));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + name + (roman.isEmpty() ? "" : " " + roman));
                meta.setLore(lore);

                // Modern API (1.20.5+) - Cleanest way to show glint without side effects
                meta.setEnchantmentGlintOverride(true);
            }

            book.setItemMeta(meta);
        }
        return book;
    }

    public int getPriorWorkPenalty(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Repairable r) {
            return r.getRepairCost();
        }
        return 0;
    }

    public void incrementPriorWorkPenalty(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Repairable r) {
            r.setRepairCost(r.getRepairCost() * 2 + 1);
            item.setItemMeta(meta);
        }
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

    public void reload() {
        loadEnchants();
    }

    // ============================================
    // EnchantConfig class
    // ============================================
    public static class EnchantConfig {
        private final String id;
        private final String displayName;
        private final String description;
        private final Material material;
        private final List<String> targets;
        private final String vanillaEnchant;
        private final String mmoitemsStat;
        private final String auraskillsStat;
        private final String attributeName;
        private final String attributeSlot;
        private final double cooldown;
        private final boolean enabled;
        private final Map<Integer, LevelConfig> levels;
        private final List<String> conflicts;
        private final int requiredEnchantingLevel;
        private final int anvilMultiplier;

        public EnchantConfig(String id, String displayName, String description, Material material,
                List<String> targets, String vanillaEnchant, String mmoitemsStat,
                String auraskillsStat, String attributeName, String attributeSlot,
                double cooldown, boolean enabled, Map<Integer, LevelConfig> levels, List<String> conflicts,
                int requiredEnchantingLevel, int anvilMultiplier) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.material = material;
            this.targets = targets;
            this.vanillaEnchant = vanillaEnchant;
            this.mmoitemsStat = mmoitemsStat;
            this.auraskillsStat = auraskillsStat;
            this.attributeName = attributeName;
            this.attributeSlot = attributeSlot;
            this.cooldown = cooldown;
            this.enabled = enabled;
            this.levels = levels;
            this.conflicts = conflicts != null ? conflicts : new ArrayList<>();
            this.requiredEnchantingLevel = requiredEnchantingLevel;
            this.anvilMultiplier = anvilMultiplier;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public Material getMaterial() {
            return material;
        }

        public List<String> getTargets() {
            return targets;
        }

        public String getVanillaEnchant() {
            return vanillaEnchant;
        }

        public String getMmoitemsStat() {
            return mmoitemsStat;
        }

        public String getAuraskillsStat() {
            return auraskillsStat;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public String getAttributeSlot() {
            return attributeSlot;
        }

        public double getCooldown() {
            return cooldown;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Map<Integer, LevelConfig> getLevels() {
            return levels;
        }

        public LevelConfig getLevel(int level) {
            return levels.get(level);
        }

        public int getMaxLevel() {
            return levels.keySet().stream().mapToInt(v -> v).max().orElse(1);
        }

        public List<String> getConflicts() {
            return conflicts;
        }

        public int getRequiredEnchantingLevel() {
            return requiredEnchantingLevel;
        }

        public int getAnvilMultiplier() {
            return anvilMultiplier;
        }
    }

    // ============================================
    // LevelConfig class
    // ============================================
    public static class LevelConfig {
        private final String value;
        private final int xpCost;

        public LevelConfig(String value, int xpCost) {
            this.value = value;
            this.xpCost = xpCost;
        }

        public String getValue() {
            return value;
        }

        public int getXpCost() {
            return xpCost;
        }

        public double getDoubleValue() {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return 0;
            }
        }
    }
}
