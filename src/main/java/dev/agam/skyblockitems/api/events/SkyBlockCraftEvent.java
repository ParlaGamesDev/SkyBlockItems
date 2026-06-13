package dev.agam.skyblockitems.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a player crafts an item through the SkyBlockItems crafting GUI
 * ({@code /craft} or custom crafting table), not vanilla crafting.
 */
public class SkyBlockCraftEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack result;
    private final int amount;

    public SkyBlockCraftEvent(Player player, ItemStack result, int amount) {
        this.player = player;
        this.result = result;
        this.amount = Math.max(1, amount);
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
