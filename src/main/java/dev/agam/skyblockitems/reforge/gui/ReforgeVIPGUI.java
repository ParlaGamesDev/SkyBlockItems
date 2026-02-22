package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.agam.skyblockitems.reforge.Reforge;
import dev.agam.skyblockitems.reforge.ReforgeApplier;
import dev.agam.skyblockitems.reforge.ReforgeGem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * VIP Reforge GUI with two input slots (Item + Gem).
 * Synced with ReforgeGUI for robustness and features.
 */
public class ReforgeVIPGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;

    private final int itemSlot = 12; // Adjusted to match screenshot
    private final int gemSlot = 14; // Adjusted to match screenshot
    private final int anvilSlot = 22;

    private int animationTask = -1;

    public ReforgeVIPGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 36, ColorUtils.colorize(getMessage("reforge.vip.gui-title")));
        setupGUI();
    }

    private String getMessage(String key) {
        return plugin.getConfigManager().getMessage(key);
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Fill entire inventory with background color first
        ItemStack filler = ColorUtils.createFillerItem(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Apply edge borders (gray by default)
        updateSidebars(Material.GRAY_STAINED_GLASS_PANE);

        // Clear input slots
        inventory.setItem(itemSlot, null);
        inventory.setItem(gemSlot, null);

        updateAnvil();
    }

    private void updateSidebars(Material material) {
        // Standard sidebar override for animations
        ItemStack glass = ColorUtils.createFillerItem(material);
        int[] sideSlots = { 0, 9, 18, 27, 8, 17, 26, 35 };
        for (int slot : sideSlots) {
            inventory.setItem(slot, glass);
        }
    }

    private void updateAnvil() {
        ItemStack item = inventory.getItem(itemSlot);
        ItemStack gemItem = inventory.getItem(gemSlot);

        ItemStack anvil = new ItemStack(Material.ANVIL);
        ItemMeta meta = anvil.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (item == null || item.getType() == Material.AIR) {
            meta.setDisplayName(ColorUtils.colorize(getMessage("reforge.vip.insert-item")));
            lore.add(ColorUtils.colorize(getMessage("reforge.vip.insert-item-lore")));
            updateSidebars(Material.GRAY_STAINED_GLASS_PANE);
        } else if (gemItem == null || gemItem.getType() == Material.AIR) {
            meta.setDisplayName(ColorUtils.colorize(getMessage("reforge.vip.insert-gem")));
            lore.add(ColorUtils.colorize(getMessage("reforge.vip.insert-gem-lore")));
            updateSidebars(Material.GRAY_STAINED_GLASS_PANE);
        } else {
            // Validate item first
            ReforgeApplier applier = new ReforgeApplier(plugin);
            if (!applier.isReforgeable(item)) {
                meta.setDisplayName(ColorUtils.colorize(getMessage(applier.getNotReforgeableReason(item) + "-title")));
                lore.add(ColorUtils.colorize(getMessage(applier.getNotReforgeableReason(item) + "-lore")));
                updateSidebars(Material.RED_STAINED_GLASS_PANE);
            } else {
                // Validate gem
                String gemId = getGemId(gemItem);
                if (gemId == null) {
                    meta.setDisplayName(ColorUtils.colorize(getMessage("reforge.vip.invalid-stone")));
                    lore.add(ColorUtils.colorize(getMessage("reforge.vip.invalid-stone-lore")));
                    updateSidebars(Material.RED_STAINED_GLASS_PANE);
                } else {
                    Reforge target = findReforgeByGem(gemId);
                    if (target == null) {
                        meta.setDisplayName(ColorUtils.colorize(getMessage("reforge.vip.unknown-stone")));
                        lore.add(ColorUtils.colorize(getMessage("reforge.vip.unknown-stone-lore")));
                        updateSidebars(Material.RED_STAINED_GLASS_PANE);
                    } else {
                        // Check compatibility
                        String itemType = getItemType(item);
                        if (!target.isCompatibleWith(itemType)) {
                            meta.setDisplayName(ColorUtils.colorize(getMessage("reforge.vip.incompatible-stone")));
                            lore.add(ColorUtils.colorize(getMessage("reforge.vip.incompatible-stone-lore")));
                            updateSidebars(Material.RED_STAINED_GLASS_PANE);
                        } else {
                            // All good!
                            meta.setDisplayName(ColorUtils.colorize(getMessage("reforge.vip.combine-button")));
                            lore.add(ColorUtils.colorize(getMessage("reforge.vip.combine-button-reforge")
                                    .replace("{reforge}", target.getDisplayName())));

                            // Get cost based on item rarity
                            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
                            dev.agam.skyblockitems.rarity.Rarity itemRarity = plugin.getRarityManager()
                                    .getRarityForItem(item, nbt);
                            String rarityId = (itemRarity != null) ? itemRarity.getIdentifier() : "COMMON";
                            double cost = target.getDataFor(rarityId).getCost();

                            lore.add(ColorUtils.colorize(getMessage("reforge.vip.combine-button-cost").replace("{cost}",
                                    String.valueOf((int) cost))));
                            lore.add("");
                            lore.add(ColorUtils.colorize(getMessage("reforge.vip.combine-button-click")));
                            updateSidebars(Material.LIME_STAINED_GLASS_PANE);
                        }
                    }
                }
            }
        }

        meta.setLore(lore);
        anvil.setItemMeta(meta);
        inventory.setItem(anvilSlot, anvil);
    }

    private String getGemId(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return null;
        return item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, "reforge_gem_id"), PersistentDataType.STRING);
    }

    private Reforge findReforgeByGem(String gemId) {
        for (Reforge r : plugin.getReforgeManager().getAllReforges()) {
            if (r.getGem() != null && r.getGem().getId().equalsIgnoreCase(gemId)) {
                return r;
            }
        }
        return null;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir())
                return;

            // 1. Blacklist check
            if (plugin.getConfigManager().isBlacklisted(clicked.getType().name())) {
                player.sendMessage(plugin.getConfigManager().getMessage("errors.blacklisted-item"));
                playSound("BLOCK_NOTE_BLOCK_BASS", 0.5f);
                startInvalidAnimation();
                return;
            }

            // 2. Identify Item Type (Gem vs Reforgeable Item)
            String gemId = getGemId(clicked);
            boolean isGem = gemId != null;

            if (isGem) {
                // GEM PLACEMENT
                ItemStack currentGem = inventory.getItem(gemSlot);
                if (currentGem == null || currentGem.getType().isAir()) {
                    ItemStack toPlace = clicked.clone();

                    // Take only exactly 1 and leave the rest
                    if (toPlace.getAmount() > 1) {
                        toPlace.setAmount(1);
                        clicked.setAmount(clicked.getAmount() - 1);
                        player.sendMessage(plugin.getConfigManager().getMessage("reforge.vip.one-item-limit"));
                    } else {
                        event.setCurrentItem(null);
                    }

                    inventory.setItem(gemSlot, toPlace);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.remove-current-first"));
                }
            } else {
                // ITEM PLACEMENT
                // FIRST: Check if the item is reforgeable
                ReforgeApplier applier = new ReforgeApplier(plugin);
                if (!applier.isReforgeable(clicked)) {
                    String reasonKey = applier.getNotReforgeableReason(clicked);
                    if (reasonKey == null)
                        reasonKey = "reforge.not-reforgeable";
                    player.sendMessage(plugin.getConfigManager().getMessage(reasonKey + "-lore"));
                    playSound("BLOCK_NOTE_BLOCK_BASS", 0.5f);
                    startInvalidAnimation();
                    return;
                }

                ItemStack currentItem = inventory.getItem(itemSlot);
                if (currentItem == null || currentItem.getType().isAir()) {
                    ItemStack toPlace = clicked.clone();

                    // Take only exactly 1 and leave the rest
                    if (toPlace.getAmount() > 1) {
                        toPlace.setAmount(1);
                        clicked.setAmount(clicked.getAmount() - 1);
                        player.sendMessage(plugin.getConfigManager().getMessage("reforge.vip.one-item-limit"));
                    } else {
                        event.setCurrentItem(null);
                    }

                    inventory.setItem(itemSlot, toPlace);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.remove-current-first"));
                }
            }
            updateAnvil();
            return;
        }

        int slot = event.getSlot();
        if (slot == itemSlot || slot == gemSlot) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                inventory.setItem(slot, null);
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack lo : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), lo);
                    }
                }
                updateAnvil();
            }
        } else if (slot == anvilSlot) {
            handleCombine();
        }
    }

    private void handleCombine() {
        ItemStack itemStack = inventory.getItem(itemSlot);
        ItemStack gemStack = inventory.getItem(gemSlot);

        if (itemStack == null || itemStack.getType().isAir() || gemStack == null || gemStack.getType().isAir()) {
            startInvalidAnimation();
            playSound("BLOCK_NOTE_BLOCK_BASS", 0.5f);
            return;
        }

        String gemId = getGemId(gemStack);
        Reforge reforge = findReforgeByGem(gemId);

        if (reforge == null) {
            startInvalidAnimation();
            playSound("BLOCK_NOTE_BLOCK_BASS", 0.5f);
            return;
        }

        // Get cost based on item rarity
        io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(itemStack);
        dev.agam.skyblockitems.rarity.Rarity itemRarity = plugin.getRarityManager().getRarityForItem(itemStack, nbt);
        String rarityId = (itemRarity != null) ? itemRarity.getIdentifier() : "COMMON";
        double cost = reforge.getDataFor(rarityId).getCost();

        // Validate cost
        if (plugin.getVaultHook() != null && !plugin.getVaultHook().hasMoney(player, cost)) {
            player.sendMessage(ColorUtils.colorize(getMessage("reforge.vip.not-enough-money")));
            playSound("BLOCK_NOTE_BLOCK_BASS", 0.5f);
            startInvalidAnimation();
            return;
        }

        // Validate compatibility
        ReforgeApplier applier = new ReforgeApplier(plugin);
        String itemType = getItemType(itemStack);
        if (!reforge.isCompatibleWith(itemType)) {
            startInvalidAnimation();
            playSound("BLOCK_NOTE_BLOCK_BASS", 0.5f);
            return;
        }

        // Deduct cost
        if (plugin.getVaultHook() != null) {
            plugin.getVaultHook().takeMoney(player, cost);
        }

        // Apply
        boolean success = applier.applyReforge(itemStack, reforge, itemType);
        if (success) {
            inventory.setItem(itemSlot, itemStack);

            // Consume 1 gem
            if (gemStack.getAmount() > 1) {
                gemStack.setAmount(gemStack.getAmount() - 1);
                inventory.setItem(gemSlot, gemStack);
            } else {
                inventory.setItem(gemSlot, null);
            }

            player.sendMessage(ColorUtils.colorize(getMessage("reforge.vip.success")
                    .replace("{reforge}", reforge.getDisplayName())
                    .replace("{gem}", reforge.getGem().getName())));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            updateAnvil();
        } else {
            player.sendMessage(ColorUtils.colorize(getMessage("reforge.error")));
            playSound("BLOCK_NOTE_BLOCK_BASS", 0.5f);
            startInvalidAnimation();
        }
    }

    private void startInvalidAnimation() {
        if (animationTask != -1) {
            Bukkit.getScheduler().cancelTask(animationTask);
            animationTask = -1;
        }

        animationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            boolean red = true;

            @Override
            public void run() {
                if (ticks >= 20 || inventory.getViewers().isEmpty()) {
                    updateAnvil();
                    Bukkit.getScheduler().cancelTask(animationTask);
                    animationTask = -1;
                    return;
                }

                if (ticks % 4 == 0) {
                    updateSidebars(red ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
                    red = !red;
                }
                ticks++;
            }
        }, 0L, 1L).getTaskId();
    }

    private void playSound(String soundName, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, pitch);
        } catch (Exception ignored) {
        }
    }

    private String getItemType(ItemStack item) {
        if (plugin.isMMOItemsEnabled()) {
            String mmoType = dev.agam.skyblockitems.integration.MMOItemsStatIntegration.getMMOItemType(item);
            if (mmoType != null)
                return mmoType.toUpperCase();
        }
        return "SWORD";
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        ItemStack item = inventory.getItem(itemSlot);
        if (item != null && item.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            leftover.values().forEach(lo -> player.getWorld().dropItemNaturally(player.getLocation(), lo));
        }

        ItemStack gem = inventory.getItem(gemSlot);
        if (gem != null && gem.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(gem);
            leftover.values().forEach(lo -> player.getWorld().dropItemNaturally(player.getLocation(), lo));
        }

        if (animationTask != -1) {
            Bukkit.getScheduler().cancelTask(animationTask);
            animationTask = -1;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
