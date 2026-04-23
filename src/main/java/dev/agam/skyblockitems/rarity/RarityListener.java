package dev.agam.skyblockitems.rarity;

import dev.agam.skyblockitems.SkyBlockItems;
import net.Indyuce.mmoitems.api.event.ItemBuildEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Event-driven rarity: {@link RarityTask}, Paper pick-block, coalesced defer in {@link RarityManager}.
 */
public class RarityListener implements Listener {

    private final SkyBlockItems plugin;
    private final RarityManager rarityManager;

    public RarityListener(SkyBlockItems plugin) {
        this.plugin = plugin;
        this.rarityManager = plugin.getRarityManager();
    }

    /**
     * True for chat-preprocess lines and {@link ServerCommandEvent} payloads that put items in the executor's inventory.
     * Strips a leading {@code /}; detects {@code execute ... run give ...} (no leading slash in server command).
     */
    private static boolean looksLikeItemGiveCommand(String message) {
        if (message == null) {
            return false;
        }
        String t = message.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("/")) {
            t = t.substring(1);
        }
        if (t.contains(" run give ") || t.contains(" run minecraft:give ")) {
            return true;
        }
        if (t.startsWith("minecraft:give")) {
            return t.length() == 14 || (t.length() > 14 && Character.isWhitespace(t.charAt(14)));
        }
        if (t.startsWith("give")) {
            return t.length() == 4 || Character.isWhitespace(t.charAt(4));
        }
        if (t.startsWith("item")) {
            return t.length() == 4 || Character.isWhitespace(t.charAt(4));
        }
        if (t.startsWith("sbi givebook") || t.startsWith("sbi givereforgegem")) {
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (!looksLikeItemGiveCommand(event.getMessage())) {
            return;
        }
        rarityManager.deferInventoryRarityApply(player);
    }

    /** {@code getCommand()} has no leading {@code /}; some Paper paths skip preprocess. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        if (!(event.getSender() instanceof Player player)) {
            return;
        }
        if (!looksLikeItemGiveCommand(event.getCommand())) {
            return;
        }
        rarityManager.deferInventoryRarityApply(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Do not merge stacks on join — preserve intentional splits across slots (same as periodic/UI passes).
                rarityManager.processInventory(player, true);
            }
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (player.getGameMode() != GameMode.CREATIVE) {
                rarityManager.processInventory(player, false);
            }
            if (rarityManager.isAllowedInventory(event.getView())) {
                Inventory top = event.getView().getTopInventory();
                if (top != null && top.getType() != InventoryType.CRAFTING
                        && top.getType() != InventoryType.CREATIVE) {
                    rarityManager.processContainerInventory(top);
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        rarityManager.markSurvivalInventoryEdited(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        rarityManager.markSurvivalInventoryEdited(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            rarityManager.processInventory(player, true);
            Inventory top = event.getView().getTopInventory();
            if (top != null && top.getHolder() instanceof Player target
                    && target != player && target.isOnline()) {
                rarityManager.processInventory(target, true);
            }
        }, 1L);
    }

    /**
     * Safety check to ensure items with different rarities never stack, 
     * even if Minecraft's NBT-based stacking check is somehow bypassed.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClickStackCheck(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType().isAir() || current == null || current.getType().isAir()) {
            return;
        }

        if (cursor.getType() != current.getType()) {
            return;
        }

        Rarity cursorRarity = rarityManager.getCurrentRarity(cursor);
        Rarity currentRarity = rarityManager.getCurrentRarity(current);

        String cursorId = cursorRarity != null ? cursorRarity.getIdentifier() : "NONE";
        String currentId = currentRarity != null ? currentRarity.getIdentifier() : "NONE";

        if (!cursorId.equalsIgnoreCase(currentId)) {
            // Rarities are different. We must prevent any stacking action.
            // Minecraft naturally swaps items with different NBT, but we block 
            // specific stacking actions to be 100% safe.
            
            switch (event.getAction()) {
                case PLACE_ALL:
                case PLACE_ONE:
                case PLACE_SOME:
                case COLLECT_TO_CURSOR:
                    event.setCancelled(true);
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        ItemStack processed = rarityManager.processItem(item);
        if (processed != item) {
            event.getItem().setItemStack(processed);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickupMonitor(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        // Same tick as pickup: item is already in the inventory — avoids 1-tick lag / “sticky” merge feel.
        rarityManager.processInventory(player, false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        ItemStack item = event.getEntity().getItemStack();
        ItemStack processed = rarityManager.processItem(item);
        if (processed != item) {
            event.getEntity().setItemStack(processed);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null || item.getType().isAir()) {
            return;
        }
        ItemStack processed = rarityManager.processItem(item);
        if (processed != item && processed != null && processed.getType() == item.getType()) {
            player.getInventory().setItem(event.getNewSlot(), processed);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.isShiftClick()) {
            return;
        }
        rarityManager.deferInventoryRarityApply(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMMOItemBuild(ItemBuildEvent event) {
        if (!plugin.isMMOItemsEnabled()) {
            return;
        }
        ItemStack item = event.getItemStack();
        Rarity target = rarityManager.getRarityForItem(item);
        if (target == null || target.getIdentifier().equalsIgnoreCase("NONE")) {
            return;
        }
        boolean custom = rarityManager.hasCustomRarity(item);
        event.setItemStack(rarityManager.applyRarity(item, target, custom));
    }

}
