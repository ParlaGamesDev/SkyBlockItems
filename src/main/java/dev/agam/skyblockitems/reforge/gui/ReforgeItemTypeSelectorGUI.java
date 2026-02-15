package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for selecting item types for reforges (based on TargetSelectorGUI
 * pattern).
 */
public class ReforgeItemTypeSelectorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<String> currentItemTypes;
    private final ReforgeEditorGUI parentGUI;

    private static final String[] AVAILABLE_ITEM_TYPES = {
            "SWORD", "BOW", "CROSSBOW", "TRIDENT", "MACE",
            "ARMOR", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS",
            "TOOL", "AXE", "PICKAXE", "SHOVEL", "HOE",
            "FISHING_ROD", "SHIELD", "CONSUMABLE", "ACCESSORY", "GLOBAL"
    };

    public ReforgeItemTypeSelectorGUI(SkyBlockItems plugin, Player player, List<String> currentItemTypes,
            ReforgeEditorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.currentItemTypes = new ArrayList<>(currentItemTypes);
        this.parentGUI = parentGUI;

        String title = plugin.getConfigManager().getMessage("reforge.editor.item-type-selector.title");
        this.inventory = Bukkit.createInventory(this, 36, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        int slot = 0;
        for (String itemType : AVAILABLE_ITEM_TYPES) {
            boolean isSelected = currentItemTypes.contains(itemType);
            Material material = getItemTypeMaterial(itemType);

            // Get localized name (Hebrew if available)
            String localizedName = plugin.getConfigManager().getMessagesConfig()
                    .getString("item-types." + itemType.toLowerCase(), itemType);

            ItemStack icon = new ItemStack(material);
            ItemMeta meta = icon.getItemMeta();

            // Display name with selection indicator
            String prefix = isSelected ? "<#2ecc71>✔ " : "<#636e72>◆ ";
            meta.setDisplayName(ColorUtils.colorize(prefix + "<#ffeaa7>" + localizedName));

            // Lore
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().getMessage(isSelected ? "reforge.editor.item-type-selector.selected"
                    : "reforge.editor.item-type-selector.not-selected"));
            lore.add("");
            lore.add(plugin.getConfigManager().getMessage("reforge.editor.item-type-selector.click-toggle"));

            meta.setLore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(slot++, icon);

            if (slot >= 27)
                break; // Leave space for bottom row
        }

        // Back button
        inventory.setItem(27, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));

        // Close/Save button
        inventory.setItem(31, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.close"), Material.EMERALD));

        // Filler
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.item-type-selector.filler-material", "YELLOW_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private Material getItemTypeMaterial(String itemType) {
        // Try config first
        String matName = plugin.getConfig().getString("item-type-icons." + itemType.toUpperCase());
        Material mat = matName != null ? Material.getMaterial(matName.toUpperCase()) : null;
        if (mat != null)
            return mat;

        // Fallback defaults
        return switch (itemType) {
            case "SWORD" -> Material.IRON_SWORD;
            case "BOW" -> Material.BOW;
            case "CROSSBOW" -> Material.CROSSBOW;
            case "TRIDENT" -> Material.TRIDENT;
            case "MACE" -> Material.MACE;
            case "ARMOR" -> Material.IRON_CHESTPLATE;
            case "HELMET" -> Material.IRON_HELMET;
            case "CHESTPLATE" -> Material.IRON_CHESTPLATE;
            case "LEGGINGS" -> Material.IRON_LEGGINGS;
            case "BOOTS" -> Material.IRON_BOOTS;
            case "TOOL" -> Material.IRON_PICKAXE;
            case "AXE" -> Material.IRON_AXE;
            case "PICKAXE" -> Material.IRON_PICKAXE;
            case "SHOVEL" -> Material.IRON_SHOVEL;
            case "HOE" -> Material.IRON_HOE;
            case "FISHING_ROD" -> Material.FISHING_ROD;
            case "SHIELD" -> Material.SHIELD;
            case "CONSUMABLE" -> Material.POTION;
            case "ACCESSORY" -> Material.EMERALD;
            case "GLOBAL" -> Material.NETHER_STAR;
            default -> Material.BARRIER;
        };
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();

        // Back/Save buttons
        if (slot == 27 || slot == 31) {
            parentGUI.updateItemTypes(currentItemTypes);
            parentGUI.reopen();
            return;
        }

        // Item type toggle
        if (slot < AVAILABLE_ITEM_TYPES.length) {
            String itemType = AVAILABLE_ITEM_TYPES[slot];

            if (currentItemTypes.contains(itemType)) {
                currentItemTypes.remove(itemType);
            } else {
                currentItemTypes.add(itemType);
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            setupGUI();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
