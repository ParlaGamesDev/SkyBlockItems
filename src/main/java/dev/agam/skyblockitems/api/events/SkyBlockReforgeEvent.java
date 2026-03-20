package dev.agam.skyblockitems.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when an item is about to be reforged (e.g. via Smithing Table or Anvil).
 * Cancelling this event prevents the reforge from being applied.
 */
public class SkyBlockReforgeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private String reforgeId;
    private boolean cancelled;

    public SkyBlockReforgeEvent(Player player, ItemStack item, String reforgeId) {
        this.player = player;
        this.item = item;
        this.reforgeId = reforgeId;
        this.cancelled = false;
    }

    public Player getPlayer() { return player; }
    public ItemStack getItem() { return item; }
    public String getReforgeId() { return reforgeId; }
    public void setReforgeId(String reforgeId) { this.reforgeId = reforgeId; }

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
