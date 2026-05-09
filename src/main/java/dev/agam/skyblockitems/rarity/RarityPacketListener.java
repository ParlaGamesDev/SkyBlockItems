package dev.agam.skyblockitems.rarity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.inventory.ItemStack;
import java.util.List;

/**
 * ProtocolLib listener that injects rarity lore visually into items
 * sent to the client. This ensures the lore is never permanently saved
 * to the item NBT on the server, preventing various lore-related bugs.
 */
public class RarityPacketListener extends PacketAdapter {

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
        PacketContainer packet = event.getPacket();
        
        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = packet.getItemModifier().read(0);
            if (shouldProcess(item)) {
                ItemStack visualItem = item.clone();
                if (applyVisualRarity(visualItem)) {
                    packet.getItemModifier().write(0, visualItem);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SET_CURSOR_ITEM) {
            ItemStack item = packet.getItemModifier().read(0);
            if (shouldProcess(item)) {
                ItemStack visualItem = item.clone();
                if (applyVisualRarity(visualItem)) {
                    packet.getItemModifier().write(0, visualItem);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            // Process the list of items
            List<ItemStack> items = packet.getItemListModifier().read(0);
            if (items != null) {
                boolean modified = false;
                for (int i = 0; i < items.size(); i++) {
                    ItemStack item = items.get(i);
                    if (shouldProcess(item)) {
                        ItemStack visualItem = item.clone();
                        if (applyVisualRarity(visualItem)) {
                            items.set(i, visualItem);
                            modified = true;
                        }
                    }
                }
                if (modified) {
                    packet.getItemListModifier().write(0, items);
                }
            }
            
            // Process the carried item (cursor item in 1.17+)
            // Some ProtocolLib versions map this to the second ItemStack modifier
            try {
                if (packet.getItemModifier().size() > 1) {
                    ItemStack carried = packet.getItemModifier().read(1);
                    if (shouldProcess(carried)) {
                        ItemStack visualItem = carried.clone();
                        if (applyVisualRarity(visualItem)) {
                            packet.getItemModifier().write(1, visualItem);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private boolean shouldProcess(ItemStack item) {
        return item != null && !item.getType().isAir();
    }

    /**
     * Applies visual rarity lore if applicable.
     * @return true if the item was modified.
     */
    private boolean applyVisualRarity(ItemStack item) {
        Rarity rarity = rarityManager.getCurrentRarity(item);
        if (rarity != null && !rarity.getIdentifier().equalsIgnoreCase("NONE")) {
            rarityManager.updateRarityLore(item, rarity);
            return true;
        }
        return false;
    }
}
