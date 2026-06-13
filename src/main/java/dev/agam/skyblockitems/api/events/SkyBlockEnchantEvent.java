package dev.agam.skyblockitems.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a player enchants an item through the SkyBlockItems enchanting GUI
 * ({@code /enchant} or custom enchanting table), not vanilla enchanting.
 */
public class SkyBlockEnchantEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack item;
    private final String enchantId;
    private final int level;

    public SkyBlockEnchantEvent(Player player, ItemStack item, String enchantId, int level) {
        this.player = player;
        this.item = item;
        this.enchantId = enchantId;
        this.level = Math.max(1, level);
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getItem() {
        return item;
    }

    public String getEnchantId() {
        return enchantId;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
