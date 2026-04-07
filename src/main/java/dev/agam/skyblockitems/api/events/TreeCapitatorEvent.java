package dev.agam.skyblockitems.api.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Custom event fired when a Tree Capitator ability is used to break multiple blocks.
 */
public class TreeCapitatorEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

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
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
