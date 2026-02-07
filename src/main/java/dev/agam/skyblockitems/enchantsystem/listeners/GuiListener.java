package dev.agam.skyblockitems.enchantsystem.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.gui.EnchantingGUI;
import dev.agam.skyblockitems.enchantsystem.gui.CustomAnvilGUI;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener for GUI events and vanilla block interaction overrides.
 * Handles all GUIs that implement BaseGUI.
 */
public class GuiListener implements Listener {

    private final SkyBlockItems plugin;

    public GuiListener(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGUI gui) {
            gui.onClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGUI gui) {
            gui.onDrag(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BaseGUI gui) {
            gui.onClose(event);
        }
    }

    /**
     * Override vanilla Enchanting Table and Anvil interactions.
     * Opens our custom GUIs instead.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        if (block == null)
            return;

        Player player = event.getPlayer();
        Material type = block.getType();

        // Override Enchanting Table
        if (type == Material.ENCHANTING_TABLE) {
            if (!player.hasPermission("skyblock.enchant")) {
                player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            } else {
                new EnchantingGUI(plugin, player).open();
            }
            event.setCancelled(true);
            return;
        }

        // Override Anvil (all variants)
        if (type == Material.ANVIL || type == Material.CHIPPED_ANVIL || type == Material.DAMAGED_ANVIL) {
            if (!player.hasPermission("skyblock.anvil")) {
                player.sendMessage(plugin.getConfigManager().getMessage("general.no-permission"));
            } else {
                new CustomAnvilGUI(plugin, player).open();
            }
            event.setCancelled(true);
        }
    }
}
