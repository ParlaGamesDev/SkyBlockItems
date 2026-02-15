package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
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
 * GUI for listing all custom enchantments.
 */
public class EnchantListGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public EnchantListGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        String title = plugin.getConfigManager().getMessage("editor.list-title");
        int size = plugin.getConfig().getInt("gui.list.size", 54);
        this.inventory = Bukkit.createInventory(this, size, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // List enchants
        List<CustomEnchant> enchants = new ArrayList<>(plugin.getCustomEnchantManager().getAllEnchants());
        int size = inventory.getSize();
        int slotsPerPage = size - 18 - (size / 9 * 2); // Roughly
        int startIndex = page * 28;
        int slot = 10;

        for (int i = startIndex; i < Math.min(startIndex + 28, enchants.size()); i++) {
            if (slot % 9 == 0)
                slot++;
            if (slot % 9 == 8)
                slot += 2;
            if (slot >= size - 10)
                break;

            CustomEnchant enchant = enchants.get(i);
            ItemStack icon = createEnchantIcon(enchant);
            inventory.setItem(slot, icon);
            slot++;
        }

        // Navigation
        if (page > 0) {
            inventory.setItem(size - 9, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.prev-page"), Material.ARROW));
        }
        if (startIndex + 28 < enchants.size()) {
            inventory.setItem(size - 1, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.next-page"), Material.ARROW));
        }

        // Create new button
        inventory.setItem(size - 5, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.create-new"), Material.EMERALD));

        // Fill remaining empty slots with configured glass pane
        Material fillerMat = Material
                .getMaterial(plugin.getConfig().getString("gui.list.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createEnchantIcon(CustomEnchant enchant) {
        ItemStack icon = new ItemStack(enchant.getMaterial());
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(enchant.getDisplayName()));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7" + (enchant.getDescription() != null ? enchant.getDescription() : "")));
            lore.add("");
            lore.add(plugin.getConfigManager().getMessage("editor.id-label").replace("{id}", enchant.getId()));
            lore.add(plugin.getConfigManager().getMessage("editor.max-level-label").replace("{max}",
                    String.valueOf(enchant.getMaxLevel())));
            lore.add(plugin.getConfigManager().getMessage("editor.targets-label").replace("{targets}",
                    String.join(", ", enchant.getTargets())));
            lore.add("");

            if (!enchant.getStats().isEmpty()) {
                lore.add(plugin.getConfigManager().getMessage("editor.stats-label"));
                for (var entry : enchant.getStats().entrySet()) {
                    lore.add(ColorUtils.colorize("  <#777777>" + entry.getKey().getHebrewName() + ": <#ffffff>+"
                            + entry.getValue() + "/level"));
                }
                lore.add("");
            }

            lore.add(plugin.getConfigManager().getMessage("editor.left-click-edit"));
            lore.add(plugin.getConfigManager().getMessage("editor.shift-right-delete"));

            meta.setLore(lore);
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
            Material navMat = Material
                    .getMaterial(plugin.getConfig().getString("gui.items.next-page.material", "ARROW"));
            if (navMat != null && clicked.getType() == navMat) {
                page++;
                setupGUI();
                return;
            }
        }

        // Create new
        if (slot == size - 5) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.prompt-id"));
            plugin.getChatInputManager().awaitInput(player, input -> {
                String id = input.toLowerCase().replace(" ", "_");
                if (plugin.getCustomEnchantManager().exists(id)) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.already-exists"));
                    return;
                }
                CustomEnchant enchant = plugin.getCustomEnchantManager().createEnchant(id);
                new EnchantEditorGUI(plugin, player, enchant).open();
            });
            return;
        }

        // Click on enchant
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

            for (CustomEnchant enchant : plugin.getCustomEnchantManager().getAllEnchants()) {
                String enchantName = ChatColor.stripColor(
                        ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));
                if (enchantName.equals(displayName)) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        // Delete
                        plugin.getCustomEnchantManager().deleteEnchant(enchant.getId());
                        player.sendMessage(
                                ColorUtils.colorize("<#ff5555>Deleted enchant: <#ffffff>" + enchant.getId()));
                        setupGUI();
                    } else {
                        // Edit
                        new EnchantEditorGUI(plugin, player, enchant).open();
                    }
                    return;
                }
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
