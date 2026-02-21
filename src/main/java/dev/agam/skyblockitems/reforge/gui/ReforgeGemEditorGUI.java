package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.reforge.ReforgeGem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for editing individual Gem properties.
 */
public class ReforgeGemEditorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final ReforgeEditorGUI parent;

    // Editable properties
    private String gemId;
    private String name;
    private List<String> lore;
    private String material;
    private int customModelData;

    public ReforgeGemEditorGUI(SkyBlockItems plugin, Player player, ReforgeGem existing, ReforgeEditorGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.parent = parent;

        if (existing != null) {
            this.gemId = existing.getId();
            this.name = existing.getName();
            this.lore = new ArrayList<>(existing.getLore());
            this.material = existing.getMaterial();
            this.customModelData = existing.getCustomModelData();
        } else {
            this.gemId = "new_gem";
            this.name = "&aNew Gem";
            this.lore = new ArrayList<>();
            this.material = "EMERALD";
            this.customModelData = 0;
        }

        this.inventory = Bukkit.createInventory(this, 36, ColorUtils.colorize("&8Edit Reforge Gem"));
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Gem ID
        inventory.setItem(10, createPropertyItem(Material.PAPER, "&bGem ID",
                Arrays.asList("&7Current: &f" + gemId, "", "&eClick to edit ID")));

        // Name
        inventory.setItem(11, createPropertyItem(Material.NAME_TAG, "&bGem Name",
                Arrays.asList("&7Current: " + name, "", "&eClick to edit Name")));

        // Lore
        List<String> loreDisplay = new ArrayList<>();
        loreDisplay.add("&7Current Lore:");
        if (lore.isEmpty())
            loreDisplay.add("  &cNone");
        else
            lore.forEach(line -> loreDisplay.add("  " + line));
        loreDisplay.add("");
        loreDisplay.add("&eClick to edit Lore");
        inventory.setItem(12, createPropertyItem(Material.BOOK, "&bGem Lore", loreDisplay));

        // Material
        inventory.setItem(13,
                createPropertyItem(
                        Material.matchMaterial(material) != null ? Material.matchMaterial(material) : Material.STONE,
                        "&bGem Material",
                        Arrays.asList("&7Current: &f" + material, "", "&eClick to edit Material")));

        // Custom Model Data
        inventory.setItem(14, createPropertyItem(Material.GOLD_NUGGET, "&bCustom Model Data",
                Arrays.asList("&7Current: &f" + customModelData, "", "&eClick to edit CMD")));

        // Delete Gem requirement
        inventory.setItem(27, createPropertyItem(Material.BARRIER, "&cRemove Gem Requirement",
                Arrays.asList("&7Clears the gem requirement.", "&7This reforge will no longer be VIP.")));

        // Save & Back
        inventory.setItem(31, createPropertyItem(Material.EMERALD_BLOCK, "&aDone",
                Arrays.asList("&7Returns to Reforge Editor.")));

        // Filler
        ItemStack filler = ColorUtils.createFillerItem(Material.GRAY_STAINED_GLASS_PANE);
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

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();

        if (slot == 10) { // ID
            prompt("Enter unique Gem ID (e.g. power_stone):", input -> {
                this.gemId = input.toLowerCase().replace(" ", "_");
                reopen();
            });
        } else if (slot == 11) { // Name
            prompt("Enter Gem Display Name:", input -> {
                this.name = input;
                reopen();
            });
        } else if (slot == 12) { // Lore
            prompt("Enter Lore lines (separated by ;):", input -> {
                this.lore = Arrays.asList(input.split(";"));
                reopen();
            });
        } else if (slot == 13) { // Material
            prompt("Enter Bukkit Material name:", input -> {
                this.material = input.toUpperCase();
                reopen();
            });
        } else if (slot == 14) { // CMD
            prompt("Enter Custom Model Data (number):", input -> {
                try {
                    this.customModelData = Integer.parseInt(input);
                } catch (Exception ignored) {
                }
                reopen();
            });
        } else if (slot == 27) { // Delete
            parent.updateGem(null);
            parent.open();
        } else if (slot == 31) { // Done
            parent.updateGem(new ReforgeGem(gemId, name, lore, material, customModelData));
            parent.open();
        }
    }

    private void prompt(String msg, java.util.function.Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage(ColorUtils.colorize("&b[Reforge Gem] &f" + msg));
        plugin.getChatInputManager().awaitInput(player, callback);
    }

    private void reopen() {
        setupGUI();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
