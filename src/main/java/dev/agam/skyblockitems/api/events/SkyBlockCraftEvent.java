package dev.agam.skyblockitems.api.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fired when a player crafts an item through the SkyBlockItems crafting GUI.
 */
public class SkyBlockCraftEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final ItemStack result;
    private final int amount;
    private final String recipeMaterial;

    public SkyBlockCraftEvent(Player player, ItemStack result, int amount) {
        this(player, result, amount, result != null ? result.getType().name() : null);
    }

    public SkyBlockCraftEvent(Player player, ItemStack result, int amount, String recipeMaterial) {
        super(player);
        this.result = result;
        this.amount = Math.max(1, amount);
        this.recipeMaterial = recipeMaterial;
    }

    public SkyBlockCraftEvent(Player player, ItemStack result, int amount, Material recipeMaterial) {
        this(player, result, amount, recipeMaterial != null ? recipeMaterial.name() : null);
    }

    public ItemStack getResult() {
        return result;
    }

    public int getAmount() {
        return amount;
    }

    /** Base recipe output type (e.g. WOODEN_SHOVEL) before rarity/MMO transforms. */
    public String getRecipeMaterial() {
        return recipeMaterial;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
