package dev.agam.skyblockitems.reforge;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.reforge.gui.ReforgeGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Event listener for the reforge GUI.
 * Handles inventory clicks and item placement.
 */
public class ReforgeListener implements Listener {

    private final SkyBlockItems plugin;

    public ReforgeListener(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ReforgeGUI gui = ReforgeGUI.getActiveGUI(player);

        if (gui == null) {
            return;
        }

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) {
            return;
        }

        int slot = event.getSlot();

        // If clicking in the GUI
        if (clickedInv.equals(gui.getInventory())) {
            // Item slot - allow item placement/removal
            if (slot == gui.getItemSlot()) {
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() != Material.AIR) {
                    event.setCancelled(true);
                    gui.getInventory().setItem(gui.getItemSlot(), null);
                    player.getInventory().addItem(item);
                    gui.updateRollButton();
                } else {
                    event.setCancelled(false);
                    // Schedule button update for next tick
                    org.bukkit.Bukkit.getScheduler().runTaskLater(
                            plugin,
                            gui::updateRollButton,
                            1L);
                }
            }
            // Roll button slot
            else if (slot == gui.getRollButtonSlot()) {
                event.setCancelled(true);
                gui.handleRoll();
            }
            // Any other slot - cancel
            else {
                event.setCancelled(true);
            }
        }
        // If clicking in player inventory while GUI is open
        else {
            // Special handling for clicking an item to move it to the reforge slot
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                ItemStack currentInSlot = gui.getInventory().getItem(gui.getItemSlot());

                // If it's a normal click (not shift) and the slot is empty
                if (!event.isShiftClick() && (currentInSlot == null || currentInSlot.getType() == Material.AIR)) {
                    event.setCancelled(true);
                    gui.getInventory().setItem(gui.getItemSlot(), clickedItem.clone());
                    clickedItem.setAmount(0);
                    gui.updateRollButton();
                }
                // If shift-clicking, already handled or will be handled
                else if (event.isShiftClick() && (currentInSlot == null || currentInSlot.getType() == Material.AIR)) {
                    event.setCancelled(true);
                    gui.getInventory().setItem(gui.getItemSlot(), clickedItem.clone());
                    clickedItem.setAmount(0);
                    gui.updateRollButton();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ReforgeGUI gui = ReforgeGUI.getActiveGUI(player);

        if (gui == null) {
            return;
        }

        // Cancel if dragging in the GUI (except item slot)
        for (int slot : event.getRawSlots()) {
            if (slot < gui.getInventory().getSize() && slot != gui.getItemSlot()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        ReforgeGUI gui = ReforgeGUI.getActiveGUI(player);

        if (gui == null) {
            return;
        }

        // Return item to player
        ItemStack item = gui.getInventory().getItem(gui.getItemSlot());
        if (item != null && item.getType() != Material.AIR) {
            player.getInventory().addItem(item);
        }

        ReforgeGUI.removeActiveGUI(player);
    }
}
