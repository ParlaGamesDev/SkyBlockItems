package dev.agam.skyblockitems.api.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Custom event fired when a Tree Capitator ability is used to break multiple blocks.
 * Now implement Cancellable to allow stopping the action.
 */
public class TreeCapitatorEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final ItemStack tool;
    private final Map<Location, Material> brokenBlocks;

    public TreeCapitatorEvent(Player player, ItemStack tool, Map<Location, Material> brokenBlocks) {
        this.player = player;
        this.tool = tool;
        this.brokenBlocks = brokenBlocks;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getTool() {
        return tool;
    }

    public Map<Location, Material> getBrokenBlocks() {
        return brokenBlocks;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
