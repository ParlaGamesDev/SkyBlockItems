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
 * GUI for editing a custom enchantment.
 */
public class EnchantEditorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final CustomEnchant enchant;

    // Slot constants
    private static final int NAME_SLOT = 11;
    private static final int DESC_SLOT = 12;
    private static final int ICON_SLOT = 13;
    private static final int TARGETS_SLOT = 14;
    private static final int MAX_LEVEL_SLOT = 15;
    private static final int REQ_LEVEL_SLOT = 16;
    private static final int STATS_SLOT = 22;
    private static final int XP_COSTS_SLOT = 23;
    private static final int CONFLICTS_SLOT = 24;
    private static final int SAVE_SLOT = 40;
    private static final int BACK_SLOT = 36;

    public EnchantEditorGUI(SkyBlockItems plugin, Player player, CustomEnchant enchant) {
        this.plugin = plugin;
        this.player = player;
        this.enchant = enchant;
        String title = plugin.getConfigManager().getMessage("editor.main-title")
                .replace("{enchant}",
                        ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName())));
        int size = plugin.getConfig().getInt("gui.editor.size", 45);
        this.inventory = Bukkit.createInventory(this, size, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Name
        inventory.setItem(NAME_SLOT,
                createEditorItem("gui.items.editor-name", enchant.getDisplayName(), Material.NAME_TAG));

        // Description
        inventory.setItem(DESC_SLOT,
                createEditorItem("gui.items.editor-description", enchant.getDescription(), Material.WRITABLE_BOOK));

        // Icon
        ItemStack iconItem = createEditorItem("gui.items.editor-icon", enchant.getMaterial().name(),
                enchant.getMaterial());
        inventory.setItem(ICON_SLOT, iconItem);

        // Targets
        inventory.setItem(TARGETS_SLOT,
                createEditorItem("gui.items.editor-targets", String.join(", ", enchant.getTargets()), Material.TARGET));

        // Max Level
        ItemStack maxLevelItem = createEditorItem("gui.items.editor-max-level", String.valueOf(enchant.getMaxLevel()),
                Material.EXPERIENCE_BOTTLE);
        ItemMeta maxLevelMeta = maxLevelItem.getItemMeta();
        if (maxLevelMeta != null) {
            List<String> lore = maxLevelMeta.hasLore() ? new ArrayList<>(maxLevelMeta.getLore()) : new ArrayList<>();
            for (String ctrl : plugin.getConfigManager().getMessages().getStringList("editor.level-controls")) {
                lore.add(ColorUtils.colorize(ctrl));
            }
            maxLevelMeta.setLore(lore);
            maxLevelItem.setItemMeta(maxLevelMeta);
        }
        inventory.setItem(MAX_LEVEL_SLOT, maxLevelItem);

        // Required Enchanting Level
        ItemStack reqLevelItem = createEditorItem("gui.items.editor-required-level",
                String.valueOf(enchant.getRequiredEnchantingLevel()), Material.BOOKSHELF);
        ItemMeta reqLevelMeta = reqLevelItem.getItemMeta();
        if (reqLevelMeta != null) {
            List<String> lore = reqLevelMeta.hasLore() ? new ArrayList<>(reqLevelMeta.getLore()) : new ArrayList<>();
            for (String ctrl : plugin.getConfigManager().getMessages().getStringList("editor.level-controls")) {
                lore.add(ColorUtils.colorize(ctrl));
            }
            reqLevelMeta.setLore(lore);
            reqLevelItem.setItemMeta(reqLevelMeta);
        }
        inventory.setItem(REQ_LEVEL_SLOT, reqLevelItem);

        // Stats
        inventory.setItem(STATS_SLOT, createEditorItem("gui.items.editor-stats", "", Material.DIAMOND_SWORD));

        // XP Costs
        inventory.setItem(XP_COSTS_SLOT, createEditorItem("gui.items.editor-xp-costs", "", Material.ENCHANTED_BOOK));

        // Conflicts
        inventory.setItem(CONFLICTS_SLOT, createEditorItem("gui.items.editor-conflicts", "", Material.BARRIER));

        // Save
        inventory.setItem(SAVE_SLOT, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.save"), Material.EMERALD_BLOCK));

        // Back
        inventory.setItem(BACK_SLOT, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));

        // Fill remaining empty slots with configured glass pane
        Material fillerMat = Material
                .getMaterial(plugin.getConfig().getString("gui.editor.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createEditorItem(String configPath, String value, Material fallback) {
        ItemStack item = ColorUtils.getItemFromConfig(plugin.getConfig().getConfigurationSection(configPath), fallback);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore().stream()
                    .map(line -> line.replace("{value}", value))
                    .map(ColorUtils::colorize)
                    .toList();
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void promptInput(String messageKey, java.util.function.Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(plugin.getConfigManager().getMessage(messageKey));
        plugin.getChatInputManager().awaitInput(player, callback);
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();

        switch (slot) {
            case NAME_SLOT -> {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage("commands.type-id")); // Reuse or specific?
                plugin.getChatInputManager().awaitInput(player, input -> {
                    enchant.setDisplayName(input);
                    new EnchantEditorGUI(plugin, player, enchant).open();
                });
            }
            case DESC_SLOT -> {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage("commands.type-id")); // Reuse or specific?
                plugin.getChatInputManager().awaitInput(player, input -> {
                    enchant.setDescription(input);
                    new EnchantEditorGUI(plugin, player, enchant).open();
                });
            }
            case ICON_SLOT -> promptInput("editor.type-material", input -> {
                Material mat = Material.getMaterial(input.toUpperCase());
                if (mat != null) {
                    enchant.setMaterial(mat);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.invalid-material"));
                }
                reopen();
            });
            case TARGETS_SLOT -> {
                new TargetSelectorGUI(plugin, player, enchant, this).open();
            }
            case MAX_LEVEL_SLOT -> {
                if (event.isLeftClick()) {
                    enchant.setMaxLevel(enchant.getMaxLevel() + 1);
                } else if (event.isRightClick()) {
                    enchant.setMaxLevel(enchant.getMaxLevel() - 1);
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                setupGUI();
            }
            case REQ_LEVEL_SLOT -> {
                if (event.isLeftClick()) {
                    enchant.setRequiredEnchantingLevel(enchant.getRequiredEnchantingLevel() + 1);
                } else if (event.isRightClick()) {
                    enchant.setRequiredEnchantingLevel(enchant.getRequiredEnchantingLevel() - 1);
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                setupGUI();
            }
            case STATS_SLOT -> {
                new StatEditorGUI(plugin, player, enchant, this).open();
            }
            case XP_COSTS_SLOT -> {
                new XPCostEditorGUI(plugin, player, enchant, this).open();
            }
            case CONFLICTS_SLOT -> {
                new ConflictSelectionGUI(plugin, player, enchant, this).open();
            }
            case SAVE_SLOT -> {
                plugin.getCustomEnchantManager().saveEnchant(enchant);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                player.sendMessage(plugin.getConfigManager().getMessage("commands.mmoenchants.saved").replace("{id}",
                        enchant.getId()));
                new EnchantListGUI(plugin, player).open();
            }
            case BACK_SLOT -> {
                new EnchantListGUI(plugin, player).open();
            }
        }

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
