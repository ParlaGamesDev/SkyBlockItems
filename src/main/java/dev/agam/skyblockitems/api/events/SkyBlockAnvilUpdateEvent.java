package dev.agam.skyblockitems.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when the CustomAnvilGUI in SkyBlockItems updates its result.
 * External plugins can use this to inject custom combination logic.
 */
public class SkyBlockAnvilUpdateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack leftItem;
    private final ItemStack rightItem;
    private ItemStack result;
    private int cost;
    private boolean cancelled;

    public SkyBlockAnvilUpdateEvent(Player player, ItemStack leftItem, ItemStack rightItem, ItemStack result, int cost) {
        this.player = player;
        this.leftItem = leftItem;
        this.rightItem = rightItem;
        this.result = result;
        this.cost = cost;
        this.cancelled = false;
    }

    public Player getPlayer() { return player; }
    public ItemStack getLeftItem() { return leftItem; }
    public ItemStack getRightItem() { return rightItem; }
    public ItemStack getResult() { return result; }
    public void setResult(ItemStack result) { this.result = result; }
    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
