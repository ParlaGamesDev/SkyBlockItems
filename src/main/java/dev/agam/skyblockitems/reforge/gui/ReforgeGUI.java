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

import java.util.*;

import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;

/**
 * GUI for the reforge system.
 * Allows players to place an item and roll for a random reforge.
 */
public class ReforgeGUI implements BaseGUI {

    private static final Map<String, ReforgeGUI> activeGUIs = new HashMap<>();
    private static final Map<UUID, Long> clickCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;

    private final int itemSlot;
    private final int rollButtonSlot;

    public ReforgeGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        // Redesigned GUI: 4 rows (36 slots)
        int size = 36;
        this.itemSlot = 13; // Row 2, Slot 5
        this.rollButtonSlot = 22; // Row 3, Slot 5

        ConfigurationSection config = plugin.getConfig().getConfigurationSection("gui.reforge");
        String title = ColorUtils
                .colorize(config != null ? config.getString("title", "<#aa55ff>&lReforge")
                        : "<#aa55ff>&lReforge");

        this.inventory = Bukkit.createInventory(this, size, title);

        setupGUI();
    }

    /**
     * Sets up the GUI with filler items and sidebars.
     */
    private void setupGUI() {
        // Initial setup with grey sidebars (empty state)
        updateSidebars(Material.GRAY_STAINED_GLASS_PANE);

        // Fill remaining empty slots with purple glass (except slots 13 and 22)
        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                if (i != itemSlot && i != rollButtonSlot) {
                    inventory.setItem(i, filler);
                }
            }
        }

        updateRollButton();
    }

    private void updateSidebars(Material material) {
        ItemStack glass = new ItemStack(material);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        // Left sidebar
        inventory.setItem(0, glass);
        inventory.setItem(9, glass);
        inventory.setItem(18, glass);
        inventory.setItem(27, glass);

        // Right sidebar
        inventory.setItem(8, glass);
        inventory.setItem(17, glass);
        inventory.setItem(26, glass);
        inventory.setItem(35, glass);
    }

    /**
     * Updates the roll button and sidebars based on current state.
     */
    public void updateRollButton() {
        ItemStack item = inventory.getItem(itemSlot);

        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("gui.items.reforge-roll");
        Material anvilMaterial = Material
                .valueOf(itemConfig != null ? itemConfig.getString("material", "ANVIL") : "ANVIL");

        ItemStack button = new ItemStack(anvilMaterial);
        ItemMeta meta = button.getItemMeta();

        if (item == null || item.getType() == Material.AIR) {
            // No item placed
            updateSidebars(Material.GRAY_STAINED_GLASS_PANE);
            meta.setDisplayName(getMessage("reforge.place-item"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().getMessage("reforge.place-item-lore"));
            meta.setLore(lore);
        } else {
            // Item placed
            ReforgeApplier applier = new ReforgeApplier(plugin);

            // First check if item is reforgeable
            if (!applier.isReforgeable(item)) {
                updateSidebars(Material.RED_STAINED_GLASS_PANE);
                String reasonKey = applier.getNotReforgeableReason(item);
                meta.setDisplayName(ColorUtils.colorize(plugin.getConfigManager().getMessage(reasonKey + "-title")));
                List<String> lore = new ArrayList<>();
                lore.add(plugin.getConfigManager().getMessage(reasonKey + "-lore"));
                meta.setLore(lore);
                button.setItemMeta(meta);
                inventory.setItem(rollButtonSlot, button);
                return;
            }

            String itemType = getItemType(item);
            String currentReforge = applier.getCurrentReforge(item);

            // Get current rarity for cost calculation
            String currentRarity = "COMMON";
            if (plugin.getRarityManager() != null) {
                dev.agam.skyblockitems.rarity.Rarity rarity = plugin.getRarityManager().getCurrentRarity(item);
                if (rarity != null) {
                    currentRarity = rarity.getIdentifier();
                }
            }

            List<Reforge> applicable = plugin.getReforgeManager().getApplicableReforges(itemType, currentRarity);

            if (applicable.isEmpty()) {
                updateSidebars(Material.RED_STAINED_GLASS_PANE);
                // Use a short title for the item name
                meta.setDisplayName(
                        ColorUtils.colorize(plugin.getConfigManager().getMessage("reforge.invalid-item-title")));
                List<String> lore = new ArrayList<>();
                // Use the detailed explanation for the lore
                lore.add(plugin.getConfigManager().getMessage("reforge.invalid-item-lore"));
                meta.setLore(lore);
            } else {
                updateSidebars(Material.LIME_STAINED_GLASS_PANE);
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

                // Pick a random reforge to show its cost
                Reforge randomReforge = applicable.get(new Random().nextInt(applicable.size()));
                int cost = (int) randomReforge.getCost();

                lore.add(getMessage("reforge.cost-label").replace("{cost}", String.valueOf(cost)));
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

        // Check for cooldown (1 second)
        long now = System.currentTimeMillis();
        long lastClick = clickCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastClick < 1000) {
            return; // Silent cancel to prevent spam
        }
        clickCooldowns.put(player.getUniqueId(), now);

        String itemType = getItemType(item);
        String currentRarity = "COMMON";
        if (plugin.getRarityManager() != null) {
            dev.agam.skyblockitems.rarity.Rarity rarity = plugin.getRarityManager().getCurrentRarity(item);
            if (rarity != null) {
                currentRarity = rarity.getIdentifier();
            }
        }

        // Get applicable reforges
        ReforgeApplier applier = new ReforgeApplier(plugin);
        String currentReforgeId = applier.getCurrentReforge(item);

        // Fixed Randomization: Ensure new reforge is different from current one
        Reforge newReforge = plugin.getReforgeManager().getRandomReforge(itemType, currentRarity, currentReforgeId);

        if (newReforge == null) {
            // Check if ANY reforge is applicable at all
            List<Reforge> allApplicable = plugin.getReforgeManager().getApplicableReforges(itemType, currentRarity);
            if (allApplicable.isEmpty()) {
                player.sendMessage(getMessage("reforge.invalid-item"));
            } else {
                // We have applicable ones, but getRandomReforge returned null because they are
                // all the current one
                player.sendMessage(getMessage("reforge.already-maximized"));
            }
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
     * Handles inventory click events.
     */
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        // Handle clicking in player inventory (to place item)
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                // Check blacklist
                if (plugin.getConfigManager().isBlacklisted(clicked.getType().name())) {
                    player.sendMessage(plugin.getConfigManager().getMessage("general.blacklisted-item"));
                    return;
                }

                // Check amount
                if (clicked.getAmount() > 1) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.one-at-a-time"));
                    return;
                }

                // Check if slot is empty
                ItemStack currentInSlot = inventory.getItem(itemSlot);
                if (currentInSlot != null && currentInSlot.getType() != Material.AIR) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.remove-current-first"));
                    return;
                }

                // Place item manually
                inventory.setItem(itemSlot, clicked.clone());
                event.setCurrentItem(null);

                // Update button state
                plugin.getServer().getScheduler().runTask(plugin, this::updateRollButton);
            }
            return;
        }

        int slot = event.getSlot();

        if (slot == rollButtonSlot) {
            handleRoll();
        } else if (slot == itemSlot) {
            // Take item back manually
            ItemStack item = inventory.getItem(itemSlot);
            if (item != null && item.getType() != Material.AIR) {
                inventory.setItem(itemSlot, null);
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack lo : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), lo);
                    }
                }
                // Update button state
                plugin.getServer().getScheduler().runTask(plugin, this::updateRollButton);
            }
        }
    }

    /**
     * Gets the item type from an ItemStack.
     */
    private String getItemType(ItemStack item) {
        // 1. Check for MMOItems Type (Most accurate for custom items)
        if (plugin.isMMOItemsEnabled()) {
            String mmoType = dev.agam.skyblockitems.integration.MMOItemsStatIntegration.getMMOItemType(item);
            if (mmoType != null && !mmoType.isEmpty()) {
                return mmoType.toUpperCase();
            }
        }

        // 2. Vanilla Fallback
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
