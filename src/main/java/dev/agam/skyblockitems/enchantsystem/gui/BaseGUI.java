package dev.agam.skyblockitems.enchantsystem.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Common interface for all plugin GUIs to ensure consistent event handling.
 */
public interface BaseGUI extends InventoryHolder {

    /**
     * Open the GUI for the player.
     */
    void open();

    /**
     * Called when a player clicks in the inventory.
     * 
     * @param event The click event
     */
    void onClick(InventoryClickEvent event);

    /**
     * Called when a player drags items in the inventory.
     * 
     * @param event The drag event
     */
    default void onDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    /**
     * Called when the inventory is closed.
     * 
     * @param event The close event
     */
    default void onClose(InventoryCloseEvent event) {
    }
}
