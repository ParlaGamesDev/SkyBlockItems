package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.reforge.Reforge;
import dev.agam.skyblockitems.reforge.ReforgeApplier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for the reforge system.
 * Allows players to place an item and roll for a random reforge.
 */
public class ReforgeGUI {

    private static final Map<String, ReforgeGUI> activeGUIs = new HashMap<>();

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;

    private final int itemSlot;
    private final int rollButtonSlot;

    public ReforgeGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Load GUI configuration
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.reforge");
        String title = ColorUtils
                .colorize(config != null ? config.getString("title", "<#aa55ff>&lשולחן חישול")
                        : "<#aa55ff>&lשולחן חישול");
        int size = config != null ? config.getInt("size", 27) : 27;
        this.itemSlot = config != null ? config.getInt("item-slot", 13) : 13;
        this.rollButtonSlot = config != null ? config.getInt("roll-button-slot", 16) : 16;

        this.inventory = Bukkit.createInventory(null, size, title);

        setupGUI();
    }

    /**
     * Sets up the GUI with filler items and the roll button.
     */
    private void setupGUI() {
        // Get filler material from config
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.reforge");
        String fillerMaterialName = config != null ? config.getString("filler-material", "PURPLE_STAINED_GLASS_PANE")
                : "PURPLE_STAINED_GLASS_PANE";
        Material fillerMaterial = Material.valueOf(fillerMaterialName);

        // Fill with glass panes
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (i != itemSlot && i != rollButtonSlot) {
                inventory.setItem(i, filler);
            }
        }

        // Add roll button
        updateRollButton();
    }

    /**
     * Updates the roll button based on current state.
     */
    public void updateRollButton() {
        ItemStack item = inventory.getItem(itemSlot);

        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("gui.items.reforge-roll");
        Material material = Material.valueOf(itemConfig != null ? itemConfig.getString("material", "ANVIL") : "ANVIL");

        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();

        if (item == null || item.getType() == Material.AIR) {
            // No item placed
            meta.setDisplayName(getMessage("reforge.place-item"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().getMessage("reforge.place-item-lore"));
            meta.setLore(lore);
        } else {
            // Item placed - show cost and current reforge if any
            String itemType = getItemType(item);
            ReforgeApplier applier = new ReforgeApplier(plugin);
            String currentReforge = applier.getCurrentReforge(item);

            List<Reforge> applicable = plugin.getReforgeManager().getApplicableReforges(itemType, "COMMON");

            if (applicable.isEmpty()) {
                meta.setDisplayName(getMessage("reforge.invalid-item"));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.getConfigManager().getMessage("reforge.invalid-item-lore"));
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ColorUtils.colorize(
                        itemConfig != null ? itemConfig.getString("name", "<#2ecc71>&lחשל!") : "<#2ecc71>&lחשל!"));

                List<String> lore = new ArrayList<>();
                if (currentReforge != null) {
                    Reforge reforge = plugin.getReforgeManager().getReforge(currentReforge);
                    if (reforge != null) {
                        lore.add(
                                getMessage("reforge.current-reforge").replace("{reforge}", reforge.getDisplayName()));
                    }
                } else {
                    lore.add(getMessage("reforge.no-reforge"));
                }
                lore.add("");

                // Show cost (use first applicable reforge's cost as example)
                double cost = applicable.isEmpty() ? 0 : applicable.get(0).getCost();
                lore.add(
                        getMessage("reforge.cost-label").replace("{cost}", String.valueOf((int) cost)));
                lore.add("");
                lore.add(plugin.getConfigManager().getMessage("reforge.click-to-reforge"));

                meta.setLore(lore);
            }
        }

        button.setItemMeta(meta);
        inventory.setItem(rollButtonSlot, button);
    }

    /**
     * Opens the GUI for the player.
     */
    public void open() {
        player.openInventory(inventory);
        activeGUIs.put(player.getName(), this);
    }

    /**
     * Closes the GUI and returns items.
     */
    public void close() {
        ItemStack item = inventory.getItem(itemSlot);
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item);
        }
        player.closeInventory();
        activeGUIs.remove(player.getName());
    }

    /**
     * Handles the reforge roll action.
     */
    public void handleRoll() {
        ItemStack item = inventory.getItem(itemSlot);

        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(getMessage("reforge.place-item"));
            playSound(player, "error");
            return;
        }

        String itemType = getItemType(item);
        String currentRarity = "COMMON"; // TODO: Get from RarityManager

        // Get applicable reforges
        ReforgeApplier applier = new ReforgeApplier(plugin);
        String currentReforgeId = applier.getCurrentReforge(item);

        Reforge newReforge = plugin.getReforgeManager().getRandomReforge(itemType, currentRarity, currentReforgeId);

        if (newReforge == null) {
            player.sendMessage(getMessage("reforge.invalid-item"));
            playSound(player, "error");
            return;
        }

        // Check economy
        if (plugin.getVaultHook() != null && plugin.getVaultHook().isEnabled()) {
            if (!plugin.getVaultHook().hasMoney(player, newReforge.getCost())) {
                String message = getMessage("reforge.not-enough-money").replace("{cost}",
                        String.valueOf((int) newReforge.getCost()));
                player.sendMessage(message);
                playSound(player, "error");
                return;
            }

            // Take the money
            plugin.getVaultHook().takeMoney(player, newReforge.getCost());
        }

        // Apply the reforge
        boolean success = applier.applyReforge(item, newReforge, itemType);

        if (success) {
            inventory.setItem(itemSlot, item);
            String message = getMessage("reforge.success").replace("{reforge}", newReforge.getDisplayName());
            player.sendMessage(message);
            playSound(player, "reforge-success");

            // Update button
            updateRollButton();
        } else {
            player.sendMessage(getMessage("reforge.error"));
            playSound(player, "error");
        }
    }

    /**
     * Gets the item type from an ItemStack.
     */
    private String getItemType(ItemStack item) {
        String materialName = item.getType().name();

        if (materialName.contains("SWORD"))
            return "SWORD";
        if (materialName.contains("BOW"))
            return "BOW";
        if (materialName.contains("CROSSBOW"))
            return "CROSSBOW";
        if (materialName.equals("TRIDENT"))
            return "TRIDENT";
        if (materialName.equals("MACE"))
            return "MACE";
        if (materialName.contains("PICKAXE"))
            return "PICKAXE";
        if (materialName.contains("AXE") && !materialName.contains("PICKAXE"))
            return "AXE";
        if (materialName.contains("SHOVEL") || materialName.contains("SPADE"))
            return "SHOVEL";
        if (materialName.contains("HOE"))
            return "HOE";
        if (materialName.contains("HELMET"))
            return "HELMET";
        if (materialName.contains("CHESTPLATE"))
            return "CHESTPLATE";
        if (materialName.contains("LEGGINGS"))
            return "LEGGINGS";
        if (materialName.contains("BOOTS"))
            return "BOOTS";

        return "UNKNOWN";
    }

    /**
     * Gets a message from messages.yml.
     */
    private String getMessage(String path) {
        return plugin.getConfigManager().getMessage(path);
    }

    /**
     * Plays a configured sound.
     */
    private void playSound(Player player, String soundKey) {
        try {
            String soundName = plugin.getConfig().getString("gui.sounds." + soundKey, "UI_BUTTON_CLICK");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            // Ignore invalid sound names
        }
    }

    // Getters for listener
    public Inventory getInventory() {
        return inventory;
    }

    public int getItemSlot() {
        return itemSlot;
    }

    public int getRollButtonSlot() {
        return rollButtonSlot;
    }

    public static ReforgeGUI getActiveGUI(Player player) {
        return activeGUIs.get(player.getName());
    }

    public static void removeActiveGUI(Player player) {
        activeGUIs.remove(player.getName());
    }
}
