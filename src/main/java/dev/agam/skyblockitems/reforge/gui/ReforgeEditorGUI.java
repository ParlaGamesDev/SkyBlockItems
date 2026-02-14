package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.reforge.Reforge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for editing individual reforge properties.
 */
public class ReforgeEditorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final String reforgeId;
    private final boolean isNew;

    // Editable properties
    private String displayName;
    private List<String> itemTypes;
    private String rarityRequirement;
    private String rarityUpgrade;
    private double cost;
    private Map<String, Double> stats;
    private List<String> enchants;
    private List<String> abilities;

    public ReforgeEditorGUI(SkyBlockItems plugin, Player player, String reforgeId, boolean isNew) {
        this.plugin = plugin;
        this.player = player;
        this.reforgeId = reforgeId;
        this.isNew = isNew;

        // Load existing data or set defaults
        if (!isNew) {
            Reforge reforge = plugin.getReforgeManager().getReforge(reforgeId);
            if (reforge != null) {
                this.displayName = reforge.getDisplayName();
                this.itemTypes = new ArrayList<>(reforge.getItemTypes());
                this.rarityRequirement = reforge.getRarityRequirement();
                this.rarityUpgrade = reforge.getRarityUpgrade();
                this.cost = reforge.getCost();
                this.stats = new HashMap<>(reforge.getStats());
                this.enchants = new ArrayList<>(reforge.getEnchants());
                this.abilities = new ArrayList<>(reforge.getAbilities());
            }
        } else {
            // Defaults for new reforge
            this.displayName = "&e" + reforgeId;
            this.itemTypes = new ArrayList<>(Arrays.asList("SWORD"));
            this.rarityRequirement = "COMMON";
            this.rarityUpgrade = "COMMON";
            this.cost = 10000;
            this.stats = new HashMap<>();
            this.enchants = new ArrayList<>();
            this.abilities = new ArrayList<>();
        }

        String title = ColorUtils
                .colorize(
                        plugin.getConfigManager().getMessage("reforge.editor.editor-title").replace("{id}", reforgeId));
        this.inventory = Bukkit.createInventory(this, 54, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Display Name
        inventory.setItem(10, createPropertyItem(Material.NAME_TAG,
                "<#ffeaa7>שם תצוגה",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי:"),
                        ColorUtils.colorize(displayName),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"))));

        // Item Types
        inventory.setItem(11, createPropertyItem(Material.IRON_SWORD,
                "<#ffeaa7>סוגי פריטים",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי:"),
                        ColorUtils.colorize("<#dfe6e9>" + String.join(", ", itemTypes)),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"),
                        ColorUtils.colorize("<#636e72>דוגמאות: SWORD, PICKAXE, BOW, ALL"))));

        // Rarity Requirement
        inventory.setItem(12, createPropertyItem(Material.PAPER,
                "<#ffeaa7>דרישת נדירות",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי: <#a29bfe>" + rarityRequirement),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"))));

        // Rarity Upgrade
        inventory.setItem(13, createPropertyItem(Material.ENCHANTED_BOOK,
                "<#ffeaa7>שדרוג נדירות",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי: <#a29bfe>" + rarityUpgrade),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"))));

        // Cost
        inventory.setItem(14, createPropertyItem(Material.GOLD_INGOT,
                "<#ffeaa7>עלות",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי: <#2ecc71>" + (int) cost + " מטבעות"),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"))));

        // Stats
        StringBuilder statsDisplay = new StringBuilder();
        if (stats.isEmpty()) {
            statsDisplay.append("<#636e72>אין סטטיסטיקות");
        } else {
            for (Map.Entry<String, Double> entry : stats.entrySet()) {
                String translatedName = plugin.getReforgeManager().formatStatName(entry.getKey());
                statsDisplay.append("\n<#dfe6e9>").append(translatedName).append(": <#2ecc71>+")
                        .append(entry.getValue());
            }
        }
        inventory.setItem(19, createPropertyItem(Material.DIAMOND,
                "<#ffeaa7>סטטיסטיקות",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי:"),
                        ColorUtils.colorize(statsDisplay.toString()),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"),
                        ColorUtils.colorize("<#636e72>פורמט: stat_name:value"))));

        // Enchants
        inventory.setItem(20, createPropertyItem(Material.ENCHANTED_BOOK,
                "<#d63aff>כישופים",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי:"),
                        ColorUtils.colorize(enchants.isEmpty() ? "<#636e72>אין כיש ופים"
                                : "<#dfe6e9>" + String.join(", ", enchants)),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"),
                        ColorUtils.colorize("<#636e72>פורמט: ENCHANT_NAME:LEVEL"))));

        // Abilities
        inventory.setItem(21, createPropertyItem(Material.BLAZE_POWDER,
                "<#00b894>יכולות",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>ערך נוכחי:"),
                        ColorUtils.colorize(abilities.isEmpty() ? "<#636e72>אין יכולות"
                                : "<#dfe6e9>" + String.join(", ", abilities)),
                        "",
                        ColorUtils.colorize("<#74b9ff>▶ לחץ לעריכה"),
                        ColorUtils.colorize("<#636e72>רשימת IDs של יכולות"))));

        // Save button
        inventory.setItem(49, createPropertyItem(Material.EMERALD_BLOCK,
                "<#2ecc71>&lשמור",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>שמור את החישול וחזור לרשימה"))));

        // Back button
        inventory.setItem(45, createPropertyItem(Material.BARRIER,
                "<#e74c3c>&lחזור",
                Arrays.asList(
                        ColorUtils.colorize("<#636e72>חזור לרשימת החישולים (ללא שמירה)"))));

        // Filler
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.list.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createPropertyItem(Material material, String name, List<String> loreLines) {
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

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Save
        if (slot == 49) {
            plugin.getReforgeManager().saveReforge(reforgeId, displayName, itemTypes,
                    rarityRequirement, rarityUpgrade, cost, stats, enchants, abilities);
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.saved")));
            new ReforgeListGUI(plugin, player).open();
            return;
        }

        // Back
        if (slot == 45) {
            new ReforgeListGUI(plugin, player).open();
            return;
        }

        // Edit Display Name
        if (slot == 10) {
            promptInput("reforge.editor.properties.display-name.prompt", input -> {
                displayName = input;
                setupGUI();
            });
        }

        // Edit Item Types
        else if (slot == 11) {
            new ReforgeItemTypeSelectorGUI(plugin, player, itemTypes, this).open();
        }

        // Edit Rarity Requirement (Cycle)
        else if (slot == 12) {
            rarityRequirement = cycleRarity(rarityRequirement);
            setupGUI();
        }

        // Edit Rarity Upgrade (Cycle)
        else if (slot == 13) {
            rarityUpgrade = cycleRarity(rarityUpgrade);
            setupGUI();
        }

        // Edit Cost
        else if (slot == 14) {
            promptInput("reforge.editor.properties.cost.prompt", input -> {
                try {
                    cost = Double.parseDouble(input);
                    setupGUI();
                } catch (NumberFormatException e) {
                    player.sendMessage(
                            ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.editor.invalid-number")));
                }
            });
        }

        // Edit Stats (Open GUI)
        else if (slot == 19) {
            new ReforgeStatEditorGUI(plugin, player, displayName, stats, this).open();
        }

        // Edit Enchants (Open GUI Selector)
        else if (slot == 20) {
            new ReforgeEnchantSelectorGUI(plugin, player, enchants, this).open();
        }

        // Edit Abilities (Open GUI Selector)
        else if (slot == 21) {
            new ReforgeAbilitySelectorGUI(plugin, player, abilities, this).open();
        }
    }

    private String cycleRarity(String current) {
        List<String> rarities = Arrays.asList("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY");
        int index = rarities.indexOf(current.toUpperCase());
        if (index == -1 || index == rarities.size() - 1) {
            return rarities.get(0);
        }
        return rarities.get(index + 1);
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

    /**
     * Updates enchants list from sub-GUI.
     */
    public void updateEnchants(List<String> newEnchants) {
        this.enchants = newEnchants;
        setupGUI();
    }

    /**
     * Updates abilities list from sub-GUI.
     */
    public void updateAbilities(List<String> newAbilities) {
        this.abilities = newAbilities;
        setupGUI();
    }

    public void reopen() {
        setupGUI();
        player.openInventory(inventory);
    }

    /**
     * Callback from ReforgeItemTypeSelectorGUI to update item types.
     */
    public void updateItemTypes(List<String> newItemTypes) {
        this.itemTypes = new ArrayList<>(newItemTypes);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
