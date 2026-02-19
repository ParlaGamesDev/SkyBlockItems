package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for selecting vanilla and custom enchantments for a reforge.
 * Similar to ConflictSelectionGUI in the CustomEnchants system.
 */
public class ReforgeEnchantSelectorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final ReforgeEditorGUI parentGUI;
    private final Map<String, Integer> selectedEnchants; // enchant:level

    public ReforgeEnchantSelectorGUI(SkyBlockItems plugin, Player player, List<String> currentEnchants,
            ReforgeEditorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.parentGUI = parentGUI;
        this.selectedEnchants = new HashMap<>();

        // Parse current enchants (format: "ENCHANT_NAME:LEVEL")
        for (String enchant : currentEnchants) {
            String[] parts = enchant.split(":");
            if (parts.length == 2) {
                try {
                    selectedEnchants.put(parts[0].toUpperCase(), Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String title = ColorUtils
                .colorize(plugin.getConfigManager().getMessage("reforge.editor.enchant-selector.title"));
        this.inventory = Bukkit.createInventory(this, 54, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Display all vanilla enchantments
        int slot = 0;
        for (Enchantment enchant : Enchantment.values()) {
            if (slot >= 45)
                break; // Leave space for bottom row

            String enchantKey = enchant.getKey().getKey().toUpperCase();
            boolean isSelected = selectedEnchants.containsKey(enchantKey);
            int level = isSelected ? selectedEnchants.get(enchantKey) : 1;

            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = item.getItemMeta();

            String statusMsg = isSelected
                    ? plugin.getConfigManager().getMessage("reforge.editor.enchant-selector.selected")
                    : plugin.getConfigManager().getMessage("reforge.editor.enchant-selector.not-selected");

            meta.setDisplayName(ColorUtils.colorize(statusMsg + " <#ffffff>" + formatEnchantName(enchantKey)));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize(
                    plugin.getConfigManager().getMessage("reforge.editor.enchant-selector.current-level")
                            .replace("{level}", String.valueOf(level))));
            lore.add("");
            lore.add(ColorUtils.colorize("&eLeft Click &7to increase level"));
            lore.add(ColorUtils.colorize("&eRight Click &7to decrease level"));
            lore.add(ColorUtils.colorize("&eShift Click &7to set level manually"));

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot++, item);
        }

        // Back button (Standardized: slot 49, Arrow material)
        inventory.setItem(49, createButton(Material.ARROW,
                plugin.getConfigManager().getMessage("gui.items.back.name"),
                plugin.getConfigManager().getMessageList("gui.items.back.lore")));

        // Filler
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.list.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 45; i < 54; i++) {
            if (i != 49 && (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR)) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Back button
        if (slot == 49) {
            returnToEditor();
            return;
        }

        // Enchantment selection
        if (slot < 45 && clicked.getType() == Material.ENCHANTED_BOOK) {
            Enchantment[] enchants = Enchantment.values();
            if (slot >= enchants.length)
                return;

            Enchantment enchant = enchants[slot];
            String enchantKey = enchant.getKey().getKey().toUpperCase();

            if (event.isShiftClick()) {
                // Set level manually via prompt
                if (selectedEnchants.containsKey(enchantKey)) {
                    player.closeInventory();
                    player.sendMessage(ColorUtils.colorize(
                            plugin.getConfigManager().getMessage("reforge.editor.enchant-selector.level-prompt")
                                    .replace("{enchant}", formatEnchantName(enchantKey))));

                    plugin.getChatInputManager().awaitInput(player, input -> {
                        try {
                            int level = Integer.parseInt(input);
                            if (level <= 0) {
                                selectedEnchants.remove(enchantKey);
                            } else {
                                selectedEnchants.put(enchantKey, level);
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(ColorUtils.colorize(
                                    plugin.getConfigManager().getMessage("reforge.editor.invalid-number")));
                        }
                        open();
                    });
                } else {
                    // Not selected, just select it
                    selectedEnchants.put(enchantKey, 1);
                    setupGUI();
                }
            } else if (event.isLeftClick()) {
                // Toggle or Increase level
                if (selectedEnchants.containsKey(enchantKey)) {
                    int currentLevel = selectedEnchants.get(enchantKey);
                    selectedEnchants.put(enchantKey, currentLevel + 1);
                } else {
                    selectedEnchants.put(enchantKey, 1);
                }
                setupGUI();
            } else if (event.isRightClick()) {
                // Decrease level or Remove
                if (selectedEnchants.containsKey(enchantKey)) {
                    int currentLevel = selectedEnchants.get(enchantKey);
                    if (currentLevel > 1) {
                        selectedEnchants.put(enchantKey, currentLevel - 1);
                    } else {
                        selectedEnchants.remove(enchantKey);
                    }
                    setupGUI();
                }
            }
        }
    }

    private void returnToEditor() {
        // Convert selectedEnchants back to List<String> format
        List<String> enchantsList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : selectedEnchants.entrySet()) {
            enchantsList.add(entry.getKey() + ":" + entry.getValue());
        }

        // Update the parent GUI
        parentGUI.updateEnchants(enchantsList);
        parentGUI.reopen();
    }

    private ItemStack createButton(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize(name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ColorUtils.colorize(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatEnchantName(String key) {
        // Convert PROTECTION_ENVIRONMENTAL -> Protection Environmental
        return Arrays.stream(key.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(key);
    }

    public void reopen() {
        setupGUI();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
