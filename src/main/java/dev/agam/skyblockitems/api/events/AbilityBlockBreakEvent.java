package dev.agam.skyblockitems.api.events;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Fired on the main thread before an ability breaks multiple blocks.
 * Used for tree mass-break abilities ({@code TREE_CAPITATOR} extra logs, {@code THUNDER_STRIKE})
 * and available for other integrations (e.g. region regrowth). Cancelling prevents those breaks.
 */
public class AbilityBlockBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final SkyBlockAbility ability;
    private final ItemStack tool;
    private final Map<Location, Material> blocks;

    public AbilityBlockBreakEvent(Player player, SkyBlockAbility ability, ItemStack tool, Map<Location, Material> blocks) {
        this.player = player;
        this.ability = ability;
        this.tool = tool;
        this.blocks = blocks;
    }

    public Player getPlayer() {
        return player;
    }

    public SkyBlockAbility getAbility() {
        return ability;
    }

    public ItemStack getTool() {
        return tool;
    }

    /**
     * @return A map of locations and materials that are about to be broken.
     */
    public Map<Location, Material> getBlocks() {
        return blocks;
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
