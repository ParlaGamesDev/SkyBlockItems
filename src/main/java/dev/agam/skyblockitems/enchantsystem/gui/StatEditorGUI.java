package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant.EnchantStat;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for editing enchantment stats.
 */
public class StatEditorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final CustomEnchant enchant;
    private final EnchantEditorGUI parent;
    private int page = 0;

    public StatEditorGUI(SkyBlockItems plugin, Player player, CustomEnchant enchant, EnchantEditorGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.enchant = enchant;
        this.parent = parent;
        String title = plugin.getConfigManager().getMessage("editor.stat-editor-title");
        int sz = plugin.getConfig().getInt("gui.stat-editor.size", 54);
        this.inventory = Bukkit.createInventory(this, sz, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // List stats with pagination
        EnchantStat[] allStats = EnchantStat.values();
        int startIndex = page * 28;
        int slot = 10;

        for (int i = startIndex; i < Math.min(startIndex + 28, allStats.length); i++) {
            if (slot % 9 == 0)
                slot++;
            if (slot % 9 == 8)
                slot += 2;
            if (slot >= inventory.getSize() - 10)
                break;

            EnchantStat stat = allStats[i];
            ItemStack icon = createStatIcon(stat);
            inventory.setItem(slot, icon);
            slot++;
        }

        // Navigation
        int invSize = inventory.getSize();
        if (page > 0) {
            inventory.setItem(invSize - 9, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.prev-page"), Material.ARROW));
        }

        if (startIndex + 28 < allStats.length) {
            inventory.setItem(invSize - 1, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.next-page"), Material.ARROW));
        }

        // Back button
        inventory.setItem(invSize - 6, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));
        inventory.setItem(invSize - 5, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.close"), Material.EMERALD));
    }

    private ItemStack createStatIcon(EnchantStat stat) {
        Material material = getStatMaterial(stat);
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();

        Double currentValue = enchant.getStats().get(stat);
        boolean isEnabled = currentValue != null && currentValue > 0;

        meta.setDisplayName(ColorUtils.colorize((isEnabled ? "&a" : "&c") + stat.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize("&7" + stat.getHebrewName()));
        lore.add("");
        lore.add(plugin.getConfigManager().getMessage("gui.stat-editor.description-label"));
        lore.add(ColorUtils.colorize("&f" + stat.getDescription()));
        lore.add("");

        if (isEnabled) {
            if (!stat.isBoolean()) {
                lore.add(plugin.getConfigManager().getMessage("gui.stat-editor.value-label", "{value}",
                        String.format("%.1f", currentValue)));
                lore.add("");
                for (String ctrl : plugin.getConfigManager().getMessageList("gui.stat-editor.controls")) {
                    lore.add(ColorUtils.colorize(ctrl));
                }
            } else {
                lore.add(plugin.getConfigManager().getMessage("gui.stat-editor.status-enabled"));
                lore.add("");
                lore.add(plugin.getConfigManager().getMessage("gui.stat-editor.click-toggle"));
            }
            lore.add(plugin.getConfigManager().getMessage("gui.stat-editor.middle-click-remove"));
        } else {
            lore.add(plugin.getConfigManager().getMessage("gui.stat-editor.not-enabled"));
            lore.add("");
            lore.add(plugin.getConfigManager().getMessage("gui.stat-editor.click-enable"));
        }

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private Material getStatMaterial(EnchantStat stat) {
        return Material.BARRIER;
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();
        int size = inventory.getSize();

        if (slot == size - 5 || slot == size - 6) {
            parent.reopen();
            return;
        }

        // Navigation
        if (slot == size - 9 && page > 0) {
            page--;
            setupGUI();
            return;
        }
        if (slot == size - 1 && event.getCurrentItem() != null) {
            Material navMat = Material.getMaterial(
                    plugin.getConfig().getString("gui.items.next-page.material", "ARROW"));
            if (navMat != null && event.getCurrentItem().getType() == navMat) {
                page++;
                setupGUI();
                return;
            }
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Find filler via config
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.stat-editor.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        if (clicked.getType() == (fillerMat != null ? fillerMat : Material.PURPLE_STAINED_GLASS_PANE))
            return;

        // Find which stat was clicked
        String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        for (EnchantStat stat : EnchantStat.values()) {
            if (stat.getDisplayName().equals(displayName)) {
                Double currentValue = enchant.getStats().get(stat);

                if (event.getClick().name().contains("MIDDLE")) {
                    // Remove stat
                    enchant.setStat(stat, 0);
                } else if (currentValue == null || currentValue <= 0) {
                    // Enable
                    enchant.setStat(stat, 1.0);
                } else if (stat.isBoolean()) {
                    // Toggle boolean
                    enchant.setStat(stat, 0);
                } else if (event.isShiftClick() && event.isRightClick()) {
                    // Increase by 10
                    enchant.setStat(stat, currentValue + 10.0);
                } else if (event.isLeftClick()) {
                    // Increase by 1
                    enchant.setStat(stat, currentValue + 1.0);
                } else if (event.isRightClick()) {
                    // Decrease by 1
                    enchant.setStat(stat, Math.max(1.0, currentValue - 1.0));
                }

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                setupGUI();
                return;
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
