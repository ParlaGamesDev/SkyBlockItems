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
 * GUI for selecting target item types.
 */
public class TargetSelectorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final CustomEnchant enchant;
    private final EnchantEditorGUI parent;

    private static final String[] AVAILABLE_TARGETS = {
            "SWORD", "BOW", "CROSSBOW", "TRIDENT", "MACE",
            "ARMOR", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS",
            "TOOL", "AXE", "FISHING_ROD", "GLOBAL"
    };

    public TargetSelectorGUI(SkyBlockItems plugin, Player player, CustomEnchant enchant, EnchantEditorGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.enchant = enchant;
        this.parent = parent;
        String title = plugin.getConfigManager().getMessage("editor.target-selector-title");
        int sz = plugin.getConfig().getInt("gui.target-selector.size", 36);
        this.inventory = Bukkit.createInventory(this, sz, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        List<String> currentTargets = enchant.getTargets();

        int slot = 0;
        for (String target : AVAILABLE_TARGETS) {
            boolean isSelected = currentTargets.contains(target);
            Material material = getTargetMaterial(target);

            // Localize target name
            String localizedTarget = plugin.getConfigManager().getMessagesConfig()
                    .getString("targets." + target.toLowerCase(), target);

            ItemStack icon = new ItemStack(material);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ColorUtils
                    .colorize((isSelected ? plugin.getConfigManager().getMessage("gui.target-selector.selected")
                            : plugin.getConfigManager().getMessage("gui.target-selector.not-selected")) + " "
                            + localizedTarget));
            meta.setLore(Arrays.asList(
                    ColorUtils
                            .colorize(isSelected ? plugin.getConfigManager().getMessage("gui.target-selector.selected")
                                    : plugin.getConfigManager().getMessage("gui.target-selector.not-selected")),
                    "",
                    plugin.getConfigManager().getMessage("gui.target-selector.click-toggle")));
            icon.setItemMeta(meta);
            inventory.setItem(slot++, icon);
        }

        // Back button
        int size = inventory.getSize();
        inventory.setItem(size - 9, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));
        inventory.setItem(size - 5, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.close"), Material.EMERALD));
    }

    private Material getTargetMaterial(String target) {
        String matName = plugin.getConfig().getString("target-icons." + target.toUpperCase());
        Material mat = matName != null ? Material.getMaterial(matName.toUpperCase()) : null;

        if (mat != null)
            return mat;

        return switch (target) {
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
            case "FISHING_ROD" -> Material.FISHING_ROD;
            case "GLOBAL" -> Material.NETHER_STAR;
            default -> Material.BARRIER;
        };
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

        if (slot < AVAILABLE_TARGETS.length) {
            String target = AVAILABLE_TARGETS[slot];
            List<String> targets = new ArrayList<>(enchant.getTargets());

            if (targets.contains(target)) {
                targets.remove(target);
            } else {
                targets.add(target);
            }

            enchant.setTargets(targets);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            setupGUI();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
