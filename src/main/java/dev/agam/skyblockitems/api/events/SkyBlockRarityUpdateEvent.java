package dev.agam.skyblockitems.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when an item is being processed for rarity application.
 * Cancelling this event prevents the item from receiving any rarity (lore/NBT).
 */
public class SkyBlockRarityUpdateEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack item;
    private boolean cancelled;

    public SkyBlockRarityUpdateEvent(ItemStack item) {
        this.item = item;
        this.cancelled = false;
    }

    public ItemStack getItem() { return item; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
