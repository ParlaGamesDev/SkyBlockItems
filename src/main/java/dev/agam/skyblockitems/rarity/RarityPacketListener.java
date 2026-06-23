package dev.agam.skyblockitems.rarity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * ProtocolLib listener that injects rarity lore visually into items
 * sent to the client. This ensures the lore is never permanently saved
 * to the item NBT on the server, preventing various lore-related bugs.
 */
public class RarityPacketListener extends PacketAdapter {

    private static final int SMELTING_RESULT_SLOT = 2;

    private final RarityManager rarityManager;

    public RarityPacketListener(SkyBlockItems plugin) {
        super(plugin, ListenerPriority.HIGHEST,
            PacketType.Play.Server.SET_SLOT,
            PacketType.Play.Server.WINDOW_ITEMS,
            PacketType.Play.Server.SET_CURSOR_ITEM);
        this.rarityManager = plugin.getRarityManager();
    }

    public static void register(SkyBlockItems plugin) {
        ProtocolLibrary.getProtocolManager().addPacketListener(new RarityPacketListener(plugin));
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SET_CURSOR_ITEM) {
            handleCursorItem(event);
            return;
        }

        if (!shouldProcessPacket(event)) {
            return;
        }

        PacketContainer packet = event.getPacket();

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            int slot = readSetSlotIndex(packet);
            ItemStack item = packet.getItemModifier().read(0);
            if (shouldProcess(item)) {
                ItemStack visualItem = item.clone();
                if (applyVisualRarity(event, visualItem, slot)) {
                    packet.getItemModifier().write(0, visualItem);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            List<ItemStack> items = packet.getItemListModifier().read(0);
            if (items != null) {
                boolean modified = false;
                for (int i = 0; i < items.size(); i++) {
                    ItemStack item = items.get(i);
                    if (shouldProcess(item)) {
                        ItemStack visualItem = item.clone();
                        if (applyVisualRarity(event, visualItem, i)) {
                            items.set(i, visualItem);
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    packet.getItemListModifier().write(0, items);
                }
            }

            try {
                if (packet.getItemModifier().size() > 1) {
                    ItemStack carried = packet.getItemModifier().read(1);
                    if (shouldProcess(carried)) {
                        ItemStack visualItem = carried.clone();
                        if (applyVisualRarity(event, visualItem, -1)) {
                            packet.getItemModifier().write(1, visualItem);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void handleCursorItem(PacketEvent event) {
        ItemStack item = event.getPacket().getItemModifier().read(0);
        if (!shouldProcess(item)) {
            return;
        }
        // Cursor is always player context; only items already stamped with rarity NBT.
        if (rarityManager.getCurrentRarity(item) == null) {
            return;
        }
        ItemStack visualItem = item.clone();
        if (applyVisualRarity(event, visualItem, -1)) {
            event.getPacket().getItemModifier().write(0, visualItem);
        }
    }

    /**
     * Skip virtual plugin menus (collections, shops, etc.). Only player inventory, physical blocks,
     * smelters, and SkyBlockItems GUIs may receive injected lore.
     */
    private boolean shouldProcessPacket(PacketEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return false;
        }

        int windowId = readWindowId(event.getPacket());
        if (windowId == 0) {
            return true;
        }

        InventoryView view = player.getOpenInventory();
        Inventory top = view.getTopInventory();
        if (top == null) {
            return false;
        }

        if (RarityManager.isSmeltingInventory(top)) {
            return true;
        }

        if (top.getHolder() instanceof BlockInventoryHolder) {
            return rarityManager.isAllowedInventory(view);
        }

        return isSkyBlockItemsGui(top);
    }

    private static boolean isSkyBlockItemsGui(Inventory inventory) {
        if (inventory.getHolder() == null) {
            return false;
        }
        return inventory.getHolder().getClass().getName().startsWith("dev.agam.skyblockitems");
    }

    private boolean shouldProcess(ItemStack item) {
        return item != null && !item.getType().isAir();
    }

    private boolean applyVisualRarity(PacketEvent event, ItemStack item, int windowSlotIndex) {
        Rarity stamped = rarityManager.getCurrentRarity(item);
        Rarity rarity = stamped;

        if ((rarity == null || rarity.getIdentifier().equalsIgnoreCase("NONE"))
                && isSmeltingResultSlot(event, windowSlotIndex)) {
            rarity = rarityManager.getRarityForItem(item);
        }

        if (rarity == null || rarity.getIdentifier().equalsIgnoreCase("NONE")) {
            return false;
        }

        // Mapped rarity (no NBT yet) is furnace-output only.
        if (stamped == null && !isSmeltingResultSlot(event, windowSlotIndex)) {
            return false;
        }

        rarityManager.updateRarityLore(item, rarity);
        return true;
    }

    private boolean isSmeltingResultSlot(PacketEvent event, int windowSlotIndex) {
        if (windowSlotIndex != SMELTING_RESULT_SLOT) {
            return false;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return false;
        }

        InventoryView view = player.getOpenInventory();
        if (!RarityManager.isSmeltingInventory(view.getTopInventory())) {
            return false;
        }

        if (SMELTING_RESULT_SLOT >= view.getTopInventory().getSize()) {
            return false;
        }

        int windowId = readWindowId(event.getPacket());
        if (windowId == 0) {
            return false;
        }

        return true;
    }

    private static int readWindowId(PacketContainer packet) {
        try {
            return packet.getIntegers().read(0);
        } catch (Exception e) {
            return -1;
        }
    }

    private static int readSetSlotIndex(PacketContainer packet) {
        try {
            if (packet.getIntegers().size() >= 3) {
                return packet.getIntegers().read(2);
            }
            if (packet.getIntegers().size() >= 2) {
                return packet.getIntegers().read(1);
            }
        } catch (Exception ignored) {
        }
        try {
            return packet.getShorts().read(0);
        } catch (Exception ignored) {
        }
        return -1;
    }
}
