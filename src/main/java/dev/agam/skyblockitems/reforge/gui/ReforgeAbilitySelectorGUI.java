package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for selecting abilities for a reforge.
 * Similar to ConflictSelectionGUI in the CustomEnchants system.
 */
public class ReforgeAbilitySelectorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final ReforgeEditorGUI parentGUI;
    private final Set<String> selectedAbilities;

    public ReforgeAbilitySelectorGUI(SkyBlockItems plugin, Player player, List<String> currentAbilities,
            ReforgeEditorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.parentGUI = parentGUI;
        this.selectedAbilities = new HashSet<>(currentAbilities);

        String title = ColorUtils
                .colorize(plugin.getConfigManager().getMessage("reforge.editor.ability-selector.title"));
        this.inventory = Bukkit.createInventory(this, 54, title);
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Get all available abilities
        Map<String, SkyBlockAbility> abilities = plugin.getAbilityManager().getAbilities();

        int slot = 0;
        for (Map.Entry<String, SkyBlockAbility> entry : abilities.entrySet()) {
            if (slot >= 45)
                break; // Leave space for bottom row

            String abilityId = entry.getKey();
            SkyBlockAbility ability = entry.getValue();
            boolean isSelected = selectedAbilities.contains(abilityId);

            ItemStack item = new ItemStack(Material.BLAZE_POWDER);
            ItemMeta meta = item.getItemMeta();

            // Format name with selection status
            String statusMsg = isSelected
                    ? plugin.getConfigManager().getMessage("reforge.editor.ability-selector.selected")
                    : plugin.getConfigManager().getMessage("reforge.editor.ability-selector.not-selected");

            meta.setDisplayName(ColorUtils.colorize(statusMsg + " §f" + formatAbilityName(abilityId)));

            List<String> lore = new ArrayList<>();

            // Add ability description if available
            List<String> description = ability.getDescription();
            if (description != null && !description.isEmpty()) {
                // Add each description line
                for (String descLine : description) {
                    lore.add(ColorUtils.colorize(
                            plugin.getConfigManager().getMessage("reforge.editor.ability-selector.description")
                                    .replace("{description}", descLine)));
                }
                lore.add("");
            }

            lore.add(ColorUtils
                    .colorize(plugin.getConfigManager().getMessage("reforge.editor.ability-selector.click-toggle")));

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot++, item);
        }

        // Back button
        inventory.setItem(49, createButton(Material.BARRIER,
                plugin.getConfigManager().getMessage("reforge.editor.buttons.back"),
                Arrays.asList(plugin.getConfigManager().getMessage("reforge.editor.buttons.back-lore"))));

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

        // Ability selection
        if (slot < 45 && clicked.getType() == Material.BLAZE_POWDER) {
            Map<String, SkyBlockAbility> abilities = plugin.getAbilityManager().getAbilities();
            List<String> abilityIds = new ArrayList<>(abilities.keySet());

            if (slot >= abilityIds.size())
                return;

            String abilityId = abilityIds.get(slot);

            // Toggle selection
            if (selectedAbilities.contains(abilityId)) {
                selectedAbilities.remove(abilityId);
            } else {
                selectedAbilities.add(abilityId);
            }

            setupGUI();
        }
    }

    private void returnToEditor() {
        // Update the parent GUI
        parentGUI.updateAbilities(new ArrayList<>(selectedAbilities));
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

    private String formatAbilityName(String id) {
        // Convert lightning_beam -> Lightning Beam
        return Arrays.stream(id.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(id);
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
