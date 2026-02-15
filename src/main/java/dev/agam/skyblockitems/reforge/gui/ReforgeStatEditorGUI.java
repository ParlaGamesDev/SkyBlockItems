package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GUI for editing individual stats of a reforge.
 */
public class ReforgeStatEditorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final String reforgeDisplayName;
    private final Map<String, Double> stats;
    private final Inventory inventory;
    private final BaseGUI parent;

    public ReforgeStatEditorGUI(SkyBlockItems plugin, Player player, String reforgeDisplayName,
            Map<String, Double> stats, BaseGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.reforgeDisplayName = reforgeDisplayName;
        this.stats = stats;
        this.parent = parent;
        this.inventory = Bukkit.createInventory(this, 54, ColorUtils.colorize(
                plugin.getConfigManager().getMessage("reforge.editor.stat-editor.title")
                        .replace("{reforge}", reforgeDisplayName)));
        setupGUI();
    }

    private void setupGUI() {
        inventory.clear();

        // Filler
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Stats
        int slot = 10;
        for (Map.Entry<String, Double> entry : stats.entrySet()) {
            if (slot > 43)
                break; // Maximum stats reached
            if (slot % 9 == 8)
                slot += 2;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            String cleanName = plugin.getReforgeManager().formatStatName(entry.getKey());
            meta.setDisplayName(ColorUtils.colorize("&e" + cleanName));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils
                    .colorize(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.stat-id-label")
                            .replace("{id}", entry.getKey())));
            lore.add(ColorUtils
                    .colorize(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.current-value")
                            .replace("{value}", String.valueOf(entry.getValue()))));
            lore.add("");
            lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessage("gui.controls.left-click")));
            lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessage("gui.controls.shift-right-delete")));
            meta.setLore(lore);

            // Store ID in PDC
            org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(new org.bukkit.NamespacedKey(plugin, "stat_id"), org.bukkit.persistence.PersistentDataType.STRING,
                    entry.getKey());

            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
        }

        // Add Stat Button
        ItemStack addStat = new ItemStack(Material.GREEN_BANNER); // Or any other material
        if (addStat.getType() == Material.AIR)
            addStat = new ItemStack(Material.EMERALD);
        ItemMeta addMeta = addStat.getItemMeta();
        addMeta.setDisplayName(
                ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.stat-editor.add-stat")));
        List<String> addLore = new ArrayList<>();
        addLore.add(
                ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.stat-editor.add-stat-lore")));
        addMeta.setLore(addLore);
        addStat.setItemMeta(addMeta);
        inventory.setItem(49, addStat);

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(
                ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.buttons.back")));
        List<String> backLore = new ArrayList<>();
        backLore.add(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.buttons.back-lore")));
        backMeta.setLore(backLore);
        back.setItemMeta(backMeta);
        inventory.setItem(45, back);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Back
        if (slot == 45) {
            parent.open();
            return;
        }

        // Add Stat (Open Selector GUI)
        if (slot == 49) {
            new ReforgeStatSelectorGUI(plugin, player, stats, this).open();
            return;
        }

        // Edit/Delete Stat
        if (slot >= 10 && slot <= 43 && clicked.getType() == Material.PAPER) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.hasLore())
                return;

            // Extract original ID from PDC
            org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
            String statId = data.get(new org.bukkit.NamespacedKey(plugin, "stat_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);

            if (statId == null)
                return;

            if (event.isShiftClick() && event.isRightClick()) {
                stats.remove(statId);
                setupGUI();
            } else if (event.isLeftClick()) {
                promptInput("reforge.editor.stat-editor.edit-value-prompt", (valueStr) -> {
                    try {
                        double value = Double.parseDouble(valueStr);
                        stats.put(statId, value);
                        setupGUI();
                    } catch (NumberFormatException ignored) {
                    }
                });
            }
        }
    }

    private void promptInput(String messageKey, java.util.function.Consumer<String> callback) {
        player.closeInventory();
        String message = plugin.getConfigManager().getMessage(messageKey);
        player.sendMessage(ColorUtils.colorize(message));
        plugin.getChatInputManager().awaitInput(player, input -> {
            callback.accept(input);
            reopen();
        });
    }

    public void reopen() {
        Bukkit.getScheduler().runTask(plugin, this::open);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}
