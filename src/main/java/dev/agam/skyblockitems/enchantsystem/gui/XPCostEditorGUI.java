package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
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
 * GUI for editing XP costs per level.
 */
public class XPCostEditorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final CustomEnchant enchant;
    private final EnchantEditorGUI parent;

    public XPCostEditorGUI(SkyBlockItems plugin, Player player, CustomEnchant enchant, EnchantEditorGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.enchant = enchant;
        this.parent = parent;
        String title = plugin.getConfigManager().getMessage("editor.xp-cost-title");
        int sz = plugin.getConfig().getInt("gui.xp-editor.size", 27);
        this.inventory = Bukkit.createInventory(this, sz, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Show levels 1-10
        for (int level = 1; level <= 10; level++) {
            int slot = level - 1;
            if (level > enchant.getMaxLevel()) {
                // Do nothing (empty slot)
            } else {
                ItemStack levelItem = new ItemStack(Material.EXPERIENCE_BOTTLE, level);
                ItemMeta meta = levelItem.getItemMeta();
                meta.setDisplayName(ColorUtils.colorize(plugin.getConfigManager()
                        .getMessage("gui.xp-editor.level-label").replace("{level}", String.valueOf(level))));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.getConfigManager().getMessage("gui.xp-editor.current-cost").replace("{cost}",
                        String.valueOf(enchant.getXpCost(level))));
                lore.add("");
                for (String ctrl : plugin.getConfigManager().getMessages().getStringList("gui.xp-editor.controls")) {
                    lore.add(ColorUtils.colorize(ctrl));
                }
                meta.setLore(lore);
                levelItem.setItemMeta(meta);
                inventory.setItem(slot, levelItem);
            }
        }

        // Back button
        int size = inventory.getSize();
        inventory.setItem(size - 9, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));
        inventory.setItem(size - 5, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.close"), Material.EMERALD));
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();
        int size = inventory.getSize();

        if (slot == size - 9 || slot == size - 5) {
            parent.reopen();
            return;
        }

        int level = slot + 1;
        if (level > enchant.getMaxLevel())
            return;

        int currentCost = enchant.getXpCost(level);
        int change = event.isShiftClick() ? 25 : 5;

        if (event.isLeftClick()) {
            enchant.setXpCost(level, currentCost + change);
        } else if (event.isRightClick()) {
            enchant.setXpCost(level, Math.max(1, currentCost - change));
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        setupGUI();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
