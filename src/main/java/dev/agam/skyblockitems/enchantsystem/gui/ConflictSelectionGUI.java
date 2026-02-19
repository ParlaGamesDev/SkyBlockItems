package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for selecting conflicting enchantments.
 */
public class ConflictSelectionGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final CustomEnchant enchant;
    private final EnchantEditorGUI parent;
    private int page = 0;
    private static final int SLOTS_PER_PAGE = 45;
    private final List<String> availableEnchantIds = new ArrayList<>();

    public ConflictSelectionGUI(SkyBlockItems plugin, Player player, CustomEnchant enchant,
            EnchantEditorGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.enchant = enchant;
        this.parent = parent;

        String title = plugin.getConfigManager().getMessage("editor.conflict-selector-title");
        this.inventory = Bukkit.createInventory(this, 54, title);

        // Collect all available enchant IDs
        availableEnchantIds.addAll(plugin.getEnchantManager().getEnchants().keySet());
        for (CustomEnchant ce : plugin.getCustomEnchantManager().getAllEnchants()) {
            if (!ce.getId().equalsIgnoreCase(enchant.getId())) {
                availableEnchantIds.add(ce.getId());
            }
        }

        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        List<String> currentConflicts = enchant.getConflicts();
        int totalEnchants = availableEnchantIds.size();
        int totalPages = (int) Math.ceil((double) totalEnchants / SLOTS_PER_PAGE);

        if (page < 0)
            page = 0;
        if (page >= totalPages && totalPages > 0)
            page = totalPages - 1;

        int startIndex = page * SLOTS_PER_PAGE;
        int endIndex = Math.min(startIndex + SLOTS_PER_PAGE, totalEnchants);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String enchantId = availableEnchantIds.get(i);
            boolean isConflict = currentConflicts.contains(enchantId);

            String displayName;
            Material material;

            EnchantConfig config = plugin.getEnchantManager().getEnchant(enchantId);
            if (config != null) {
                displayName = config.getDisplayName();
                material = config.getMaterial();
            } else {
                CustomEnchant ce = plugin.getCustomEnchantManager().getEnchant(enchantId);
                displayName = ce != null ? ce.getDisplayName() : enchantId;
                material = ce != null ? ce.getMaterial() : Material.BOOK;
            }

            ItemStack icon = new ItemStack(material);
            ItemMeta meta = icon.getItemMeta();

            if (meta != null) {
                String status = isConflict ? plugin.getConfigManager().getMessage("gui.conflict-selector.selected")
                        : plugin.getConfigManager().getMessage("gui.conflict-selector.not-selected");

                meta.setDisplayName(ColorUtils.colorize(status + " " + displayName));
                meta.setLore(Arrays.asList(
                        "",
                        plugin.getConfigManager().getMessage("gui.conflict-selector.click-toggle")));

                icon.setItemMeta(meta);
            }
            inventory.setItem(slot++, icon);
        }

        // Navigation
        if (page > 0) {
            inventory.setItem(45, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.prev-page"), Material.ARROW));
        }

        if (startIndex + SLOTS_PER_PAGE < totalEnchants) {
            inventory.setItem(53, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.next-page"), Material.ARROW));
        }

        // Back / Save (Standardized: slot 49, Arrow material)
        inventory.setItem(49, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));

        // Fill remaining empty slots with configured glass pane
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.conflict-selector.filler-material", "RED_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();

        // Back / Save
        if (slot == 49) {
            parent.open();
            return;
        }

        // Navigation
        if (slot == 45 && page > 0) {
            page--;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            setupGUI();
            return;
        }

        if (slot == 53) {
            int totalPages = (int) Math.ceil((double) availableEnchantIds.size() / SLOTS_PER_PAGE);
            if (page < totalPages - 1) {
                page++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                setupGUI();
            }
            return;
        }

        // Toggle Conflict
        int index = (page * SLOTS_PER_PAGE) + slot;
        if (slot >= 0 && slot < SLOTS_PER_PAGE && index < availableEnchantIds.size()) {
            String clickedId = availableEnchantIds.get(index);
            List<String> conflicts = new ArrayList<>(enchant.getConflicts());

            if (conflicts.contains(clickedId)) {
                conflicts.remove(clickedId);
            } else {
                conflicts.add(clickedId);
            }

            enchant.setConflicts(conflicts);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            setupGUI();
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
