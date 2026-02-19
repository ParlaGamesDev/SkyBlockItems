package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.LevelConfig;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Sub-menu for selecting enchantment level.
 * Shows the item and all available levels to choose from.
 */
public class LevelSelectionGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final ItemStack itemToEnchant;
    private final EnchantConfig enchant;
    private final EnchantingGUI parentGui;
    private boolean returningToParent = false;
    private boolean processingPurchase = false;

    private static final int ITEM_SLOT = 4; // Top center - shows the item
    private static final int INFO_SLOT = 22; // Center - shows enchant info

    // Level slots - arranged nicely in the GUI
    private static final int[] LEVEL_SLOTS = {
            19, 20, 21, 22, 23, 24, 25, // Row 3: levels 1-7 (shifted from row 4)
            28, 29, 30 // Row 4: levels 8-10 (shifted from row 5)
    };

    public LevelSelectionGUI(SkyBlockItems plugin, Player player, ItemStack itemToEnchant,
            EnchantConfig enchant, EnchantingGUI parentGui) {
        this.plugin = plugin;
        this.player = player;
        this.itemToEnchant = itemToEnchant;
        this.enchant = enchant;
        this.parentGui = parentGui;

        String title = plugin.getConfigManager().getMessage("enchanting.level-select.title")
                .replace("{enchant}",
                        ChatColor.stripColor(ColorUtils.colorize(enchant.getDisplayName())));

        int menuSize = plugin.getConfig().getInt("gui.level-select.size", 54);
        this.inventory = Bukkit.createInventory(this, menuSize, title);
        setupGui();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGui() {
        // Place the item being enchanted at the top
        inventory.setItem(ITEM_SLOT, itemToEnchant.clone());

        // Get current level of this enchant on the item
        int currentLevel = getCurrentEnchantLevel();

        // Place level buttons
        for (int lvl = 1; lvl <= 10; lvl++) {
            int slotIndex = lvl - 1;
            if (slotIndex >= LEVEL_SLOTS.length)
                break;

            LevelConfig levelConfig = enchant.getLevel(lvl);
            if (levelConfig == null)
                continue;

            ItemStack levelIcon = createLevelIcon(lvl, levelConfig, currentLevel);
            inventory.setItem(LEVEL_SLOTS[slotIndex], levelIcon);
        }

        // Back button (Standardized: Arrow material)
        Material backMat = Material
                .getMaterial(plugin.getConfig().getString("gui.level-select.back-material", "ARROW"));
        ItemStack backButton = new ItemStack(backMat != null ? backMat : Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName(plugin.getConfigManager().getMessage("gui.items.back.name"));
        backButton.setItemMeta(backMeta);
        inventory.setItem(plugin.getConfig().getInt("gui.level-select.back-slot", 49), backButton);

        // Fill remaining empty slots with configured glass pane
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.level-select.filler-material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createLevelIcon(int level, LevelConfig levelConfig, int currentLevel) {
        int pwp = plugin.getEnchantManager().getPriorWorkPenalty(itemToEnchant);
        int totalCost = levelConfig.getXpCost() + pwp;

        boolean isUpgrade = level > currentLevel;
        boolean canAffordXp = player.getLevel() >= totalCost;
        boolean canBuy = isUpgrade && canAffordXp;

        // Use different materials based on status
        Material material;
        int maxCost = plugin.getConfig().getInt("enchanting.max-cost", 50);
        boolean limitEnabled = plugin.getConfig().getBoolean("enchanting.limit-enabled", true);
        boolean bypassOp = plugin.getConfig().getBoolean("enchanting.bypass-limits-on-op", true);
        boolean tooExpensive = limitEnabled && totalCost > maxCost && !(player.isOp() && bypassOp);

        if (!isUpgrade) {
            material = Material.GRAY_STAINED_GLASS_PANE; // Already have this level or higher
        } else if (tooExpensive) {
            material = Material.RED_STAINED_GLASS_PANE; // Too expensive
        } else if (canBuy) {
            material = Material.LIME_STAINED_GLASS_PANE; // Can afford
        } else {
            material = Material.RED_STAINED_GLASS_PANE; // Can't afford
        }

        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();

        String displayName = plugin.getConfigManager().getMessage("enchanting.level-button.name", "{level}",
                toRoman(level));
        meta.setDisplayName(ColorUtils.colorize(displayName));

        List<String> lore = new ArrayList<>();

        // Description
        String desc = enchant.getDescription();
        if (desc.contains("\n")) {
            for (String line : desc.split("\n")) {
                lore.add(ColorUtils.colorize("&7" + line.trim()));
            }
        } else {
            lore.add(ColorUtils.colorize("&7" + desc));
        }

        // Cost info

        lore.add(plugin.getConfigManager().getMessage("enchanting.level-button.xp-cost", "{cost}",
                String.valueOf(totalCost)));

        lore.add("");

        // Status message
        if (!isUpgrade) {
            lore.add(plugin.getConfigManager().getMessage("enchanting.level-button.already-have"));
        } else if (!canAffordXp) {
            lore.add(plugin.getConfigManager().getMessage("enchanting.level-button.need-xp", "{need}",
                    String.valueOf(totalCost - player.getLevel())));
        } else if (tooExpensive) {
            lore.add(plugin.getConfigManager().getMessage("enchanting.too-expensive-lore", "{max}",
                    String.valueOf(maxCost)));
        } else {
            lore.add(plugin.getConfigManager().getMessage("enchanting.level-button.click-to-buy"));
        }

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private int getCurrentEnchantLevel() {
        // Check vanilla enchant
        if (enchant.getVanillaEnchant() != null) {
            Enchantment vanillaEnchant = Enchantment.getByName(enchant.getVanillaEnchant());
            if (vanillaEnchant != null && itemToEnchant.getItemMeta().hasEnchant(vanillaEnchant)) {
                return itemToEnchant.getItemMeta().getEnchantLevel(vanillaEnchant);
            }
        }

        // Check lore (comma-separated format)
        if (itemToEnchant.hasItemMeta() && itemToEnchant.getItemMeta().hasLore()) {
            String cleanEnchant = ChatColor.stripColor(
                    ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));

            for (String line : itemToEnchant.getItemMeta().getLore()) {
                String cleanLine = ChatColor.stripColor(line);
                // Split by comma to check each enchant entry
                String[] entries = cleanLine.split(",");
                for (String entry : entries) {
                    String trimmed = entry.trim();
                    if (trimmed.startsWith(cleanEnchant + " ")) {
                        // Extract roman numeral after enchant name
                        String lvlStr = trimmed.substring(cleanEnchant.length()).trim();
                        return fromRoman(lvlStr);
                    }
                }
            }
        }

        return 0;
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        if (processingPurchase)
            return;

        int slot = event.getSlot();

        // Handle item slot click (to take item back)
        if (slot == ITEM_SLOT) {
            if (itemToEnchant != null && itemToEnchant.getType() != Material.AIR) {
                returningToParent = true;
                ItemStack toReturn = itemToEnchant;

                // Return to player
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toReturn);
                if (!leftover.isEmpty()) {
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }

                // Sync with parent GUI
                parentGui.setItemToEnchant(null);
                parentGui.reopenFromLevelSelection(player, null);

                player.sendMessage(plugin.getConfigManager().getMessage("enchanting.item-returned"));
            }
            return;
        }

        // Prevent clicking player inventory items
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            return;
        }

        // Back button
        if (slot == plugin.getConfig().getInt("gui.level-select.back-slot", 49)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            this.returningToParent = true;
            parentGui.reopenFromLevelSelection(player, itemToEnchant);
            return;
        }

        // Check if clicked on a level slot
        for (int i = 0; i < LEVEL_SLOTS.length; i++) {
            if (slot == LEVEL_SLOTS[i]) {
                int level = i + 1;
                handleLevelClick(level);
                return;
            }
        }
    }

    private void handleLevelClick(int level) {
        if (processingPurchase)
            return;

        LevelConfig levelConfig = enchant.getLevel(level);
        if (levelConfig == null)
            return;

        int currentLevel = getCurrentEnchantLevel();

        // Check if upgrade
        if (level <= currentLevel) {
            player.sendMessage(plugin.getConfigManager().getMessage("enchanting.upgrade-fail"));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
            return;
        }

        // Check for conflicts
        String conflictWith = plugin.getEnchantManager().getConflict(itemToEnchant, enchant);
        if (conflictWith != null) {
            player.sendMessage(
                    plugin.getConfigManager().getMessage("errors.conflict-error", "{enchant}", conflictWith));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        // Check XP levels
        int pwp = plugin.getEnchantManager().getPriorWorkPenalty(itemToEnchant);
        int totalCost = levelConfig.getXpCost() + pwp;

        // Check if too expensive
        int maxCost = plugin.getConfig().getInt("enchanting.max-cost", 50);
        boolean limitEnabled = plugin.getConfig().getBoolean("enchanting.limit-enabled", true);
        boolean bypassOp = plugin.getConfig().getBoolean("enchanting.bypass-limits-on-op", true);

        if (limitEnabled && totalCost > maxCost && !(player.isOp() && bypassOp)) {
            player.sendMessage(plugin.getConfigManager().getMessage("enchanting.too-expensive", "{max}",
                    String.valueOf(maxCost)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        if (player.getLevel() < totalCost) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessageRaw("errors.not-enough-xp")
                    .replace("{cost}", String.valueOf(totalCost))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        // Apply enchantment
        processingPurchase = true;

        applyEnchantment(level, levelConfig);
        player.setLevel(player.getLevel() - totalCost);

        // Update PWP
        plugin.getEnchantManager().incrementPriorWorkPenalty(itemToEnchant);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);

        String romanValue = (enchant.getMaxLevel() == 1) ? "" : " " + toRoman(level);
        String successMsg = plugin.getConfigManager().getMessageRaw("enchanting.success")
                .replace("{enchant}", enchant.getDisplayName())
                .replace("{level}", romanValue)
                .replace("{cost}", String.valueOf(totalCost));
        player.sendMessage(ColorUtils.colorize(successMsg));

        // Return to player and close GUI
        this.returningToParent = false;
        player.closeInventory();
    }

    private void applyEnchantment(int level, LevelConfig levelConfig) {
        ItemMeta meta = itemToEnchant.getItemMeta();
        List<String> oldLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Apply vanilla enchantment if defined
        boolean hasVanillaEnchant = false;
        if (enchant.getVanillaEnchant() != null) {
            Enchantment vanillaEnchant = Enchantment.getByName(enchant.getVanillaEnchant());
            if (vanillaEnchant != null) {
                meta.addEnchant(vanillaEnchant, level, true);
                hasVanillaEnchant = true;
            }
        }

        // Apply cosmetic glow for custom enchants that don't have a vanilla enchant
        if (!hasVanillaEnchant) {
            meta.setEnchantmentGlintOverride(true);
        }

        // Apply attribute modifier
        if (enchant.getAttributeName() != null) {
            try {
                Attribute attribute = Attribute.valueOf(enchant.getAttributeName());
                EquipmentSlot slot = EquipmentSlot.valueOf(enchant.getAttributeSlot());
                NamespacedKey key = new NamespacedKey(plugin,
                        "enchant_" + enchant.getId() + "_" + UUID.randomUUID().toString().substring(0, 8));
                EquipmentSlotGroup slotGroup = getSlotGroup(slot);
                AttributeModifier modifier = new AttributeModifier(key, levelConfig.getDoubleValue(),
                        AttributeModifier.Operation.ADD_NUMBER, slotGroup);
                meta.addAttributeModifier(attribute, modifier);
            } catch (Exception ignored) {
            }
        }

        // Update lore
        List<String> enchantEntries = new ArrayList<>();
        List<String> regularLore = new ArrayList<>();
        String enchantNameClean = ChatColor
                .stripColor(ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));

        for (String line : oldLore) {
            String lineClean = ChatColor.stripColor(line);

            if (line.startsWith(ChatColor.GRAY.toString()) && isEnchantLine(lineClean)) {
                String[] parts = lineClean.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith(enchantNameClean)) {
                        enchantEntries.add(trimmed);
                    }
                }
            } else {
                regularLore.add(line);
            }
        }

        // Add the current enchantment
        String romanValue = (enchant.getMaxLevel() == 1) ? "" : " " + toRoman(level);
        enchantEntries.add(enchantNameClean + romanValue);

        // Build new lore
        List<String> newLore = new ArrayList<>();

        // Add enchants (3 per line)
        StringBuilder currentLine = new StringBuilder();
        int count = 0;
        for (int i = 0; i < enchantEntries.size(); i++) {
            if (count > 0)
                currentLine.append(", ");
            currentLine.append(enchantEntries.get(i));
            count++;

            if (count == 3 || i == enchantEntries.size() - 1) {
                newLore.add(ChatColor.GRAY + currentLine.toString());
                currentLine = new StringBuilder();
                count = 0;
            }
        }

        // Add Regular Lore
        newLore.addAll(regularLore);

        meta.setLore(newLore);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        itemToEnchant.setItemMeta(meta);
    }

    private boolean isEnchantLine(String text) {
        return text.matches("(?i).*\\b(I|II|III|IV|V|VI|VII|VIII|IX|X)\\b.*");
    }

    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);
    }

    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this)
            return;

        // If not returning to parent and item still exists, return it
        if (!returningToParent && itemToEnchant != null && itemToEnchant.getType() != Material.AIR
                && itemToEnchant.getAmount() > 0) {

            // Give back to player
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemToEnchant);
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }

            // Explicitly clear item to prevent double-giving if event fires again
            itemToEnchant.setAmount(0);
        }
    }

    private EquipmentSlotGroup getSlotGroup(EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> EquipmentSlotGroup.MAINHAND;
            case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ANY;
        };
    }

    private String toRoman(int n) {
        return switch (n) {
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
            default -> String.valueOf(n);
        };
    }

    private int fromRoman(String roman) {
        if (roman == null)
            return 0;
        return switch (roman.toUpperCase()) {
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
