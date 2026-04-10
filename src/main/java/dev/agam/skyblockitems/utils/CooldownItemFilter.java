package dev.agam.skyblockitems.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketContainer;
import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * ProtocolLib filter that strips {@code minecraft:use_cooldown} from grappling-hook items in
 * outgoing inventory packets. For fishing rods, the component is never sent: the client would
 * otherwise trigger the radial sweep on every cast/reel (1.21.2+). Visual cooldown uses
 * {@code ClientboundCooldownPacket} with the custom cooldown group instead.
 */
public class CooldownItemFilter {

    public static void register() {
        if (!SkyBlockItems.getInstance().getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            return;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                SkyBlockItems.getInstance(),
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.SET_SLOT,
                PacketType.Play.Server.WINDOW_ITEMS
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return;

                PacketContainer packet = event.getPacket();
                
                if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                    ItemStack item = packet.getItemModifier().read(0);
                    if (processItem(player, item)) {
                        packet.getItemModifier().write(0, item);
                    }
                } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                    List<ItemStack> items = packet.getItemListModifier().read(0);
                    boolean modified = false;
                    for (int i = 0; i < items.size(); i++) {
                        if (processItem(player, items.get(i))) {
                            modified = true;
                        }
                    }
                    if (modified) {
                        packet.getItemListModifier().write(0, items);
                    }
                }
            }
        });
    }

    private static boolean processItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        // Only target items tagged as the Grappling Hook
        NBTItem nbt = NBTItem.get(item);
        if (nbt != null && nbt.hasTag("SKYBLOCK_GRAPPLING_HOOK")) {
            if (item.getType() == Material.FISHING_ROD) {
                VisualCooldownUtils.clearCooldownComponent(item);
                return true;
            }
            if (!VisualCooldownUtils.hasActiveCooldown(player.getUniqueId(), "GRAPPLING_HOOK")) {
                VisualCooldownUtils.clearCooldownComponent(item);
                return true;
            }
        }
        return false;
    }
}
