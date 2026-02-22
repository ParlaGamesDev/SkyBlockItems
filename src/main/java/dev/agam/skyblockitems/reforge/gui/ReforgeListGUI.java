package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.reforge.Reforge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for listing all custom reforges with create/edit/delete capabilities.
 * Similar to EnchantListGUI but for the reforge system.
 */
public class ReforgeListGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public ReforgeListGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        String title = ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.list-title"));
        int size = 54;
        this.inventory = Bukkit.createInventory(this, size, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // List reforges
        List<Reforge> reforges = new ArrayList<>(plugin.getReforgeManager().getAllReforges());
        reforges.sort(Comparator.comparing(Reforge::getId));

        int size = inventory.getSize();
        int startIndex = page * 28;
        int slot = 10;

        for (int i = startIndex; i < Math.min(startIndex + 28, reforges.size()); i++) {
            if (slot % 9 == 0)
                slot++;
            if (slot % 9 == 8)
                slot += 2;
            if (slot >= size - 10)
                break;

            Reforge reforge = reforges.get(i);
            ItemStack icon = createReforgeIcon(reforge);
            inventory.setItem(slot, icon);
            slot++;
        }

        // Navigation
        if (page > 0) {
            inventory.setItem(size - 9, ColorUtils.getItemFromConfig(
                    plugin.getConfigManager().getMessages().getConfigurationSection("gui.items.prev-page"),
                    Material.ARROW));
        }
        if (startIndex + 28 < reforges.size()) {
            inventory.setItem(size - 1, ColorUtils.getItemFromConfig(
                    plugin.getConfigManager().getMessages().getConfigurationSection("gui.items.next-page"),
                    Material.ARROW));
        }

        // Create new button
        ItemStack createNew = new ItemStack(Material.EMERALD);
        ItemMeta meta = createNew.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.create-new")));
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.create-new-lore")));
        meta.setLore(lore);
        createNew.setItemMeta(meta);
        inventory.setItem(size - 5, createNew);

        // Fill remaining empty slots
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.list.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createReforgeIcon(Reforge reforge) {
        ItemStack icon = new ItemStack(Material.ANVIL);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(reforge.getDisplayName()));

            List<String> lore = new ArrayList<>();
            Reforge.RarityData commonData = reforge.getDataFor("COMMON");
            lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.labels.id-label")
                    .replace("{id}", reforge.getId())));
            lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.labels.cost-label")
                    .replace("{cost}", String.valueOf((int) reforge.getDataFor("COMMON").getCost()))));

            if (reforge.hasGem()) {
                lore.add(ColorUtils.colorize(" <#fdcb6e>⚡ VIP • Requires: " + reforge.getGem().getName()));
            }

            lore.add("");
            lore.add(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.labels.types-label")
                    .replace("{types}", String.join(", ", reforge.getItemTypes()))));
            lore.add("");

            if (!commonData.getStats().isEmpty()) {
                lore.add(ColorUtils
                        .colorize(plugin.getConfigManager().getMessage("reforge.editor.labels.stats-header")));
                for (Map.Entry<String, Double> entry : commonData.getStats().entrySet()) {
                    lore.add(ColorUtils
                            .colorize(
                                    "  <#636e72>• <#dfe6e9>" + plugin.getReforgeManager().formatStatName(entry.getKey())
                                            + ": <#2ecc71>+" + entry.getValue()));
                }
                lore.add("");
            }

            if (!commonData.getEnchants().isEmpty()) {
                lore.add(ColorUtils
                        .colorize(plugin.getConfigManager().getMessage("reforge.editor.labels.enchants-header")
                                .replace("{enchants}", String.join(", ", commonData.getEnchants()))));
            }

            lore.add("");
            lore.add(ColorUtils
                    .colorize(plugin.getConfigManager().getMessage("reforge.editor.buttons.left-click-edit")));
            lore.add(ColorUtils
                    .colorize(plugin.getConfigManager().getMessage("reforge.editor.buttons.shift-right-delete")));

            meta.setLore(lore);

            // Store ID in PDC
            org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(new org.bukkit.NamespacedKey(plugin, "reforge_id"),
                    org.bukkit.persistence.PersistentDataType.STRING, reforge.getId());

            icon.setItemMeta(meta);
        }
        return icon;
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();
        int size = inventory.getSize();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Navigation
        if (slot == size - 9 && page > 0) {
            page--;
            setupGUI();
            return;
        }
        if (slot == size - 1 && clicked != null) {
            page++;
            setupGUI();
            return;
        }

        // Create new
        if (slot == size - 5) {
            player.closeInventory();
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.prompt-id")));
            plugin.getChatInputManager().awaitInput(player, input -> {
                String id = input.toLowerCase().replace(" ", "_");
                if (plugin.getReforgeManager().getReforge(id) != null) {
                    player.sendMessage(
                            ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.already-exists")));
                    return;
                }
                new ReforgeEditorGUI(plugin, player, id, true).open();
            });
            return;
        }

        // Click on reforge
        if (clicked.hasItemMeta()) {
            org.bukkit.persistence.PersistentDataContainer data = clicked.getItemMeta().getPersistentDataContainer();
            String reforgeId = data.get(new org.bukkit.NamespacedKey(plugin, "reforge_id"),
                    org.bukkit.persistence.PersistentDataType.STRING);

            if (reforgeId != null) {
                Reforge reforge = plugin.getReforgeManager().getReforge(reforgeId);

                if (reforge != null) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        // Delete
                        deleteReforge(reforgeId);
                        player.sendMessage(ColorUtils.colorize(
                                plugin.getConfigManager().getMessage("reforge.editor.deleted").replace("{id}",
                                        reforgeId)));
                        setupGUI();
                    } else {
                        // Edit
                        new ReforgeEditorGUI(plugin, player, reforgeId, false).open();
                    }
                }
            }
        }
    }

    private void deleteReforge(String id) {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getReforgeManager().getConfig();
        config.set("reforges." + id, null);
        plugin.getReforgeManager().saveConfig();
        plugin.getReforgeManager().reload();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
