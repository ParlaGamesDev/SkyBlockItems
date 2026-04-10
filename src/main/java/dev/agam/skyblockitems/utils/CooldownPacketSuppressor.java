package dev.agam.skyblockitems.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intercepts and suppresses premature visual cooldown packets for the Grappling Hook.
 * Prevents other plugins or the game from triggering the cooldown sweep on rod cast.
 */
public class CooldownPacketSuppressor {

    private static final Map<UUID, Long> castingPhasePlayers = new ConcurrentHashMap<>();
    private static final long CAST_WINDOW_MS = 300; // Reduced window to ensure it doesn't block the actual grapple

    public static void register() {
        if (!SkyBlockItems.getInstance().getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            return;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                SkyBlockItems.getInstance(),
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.SET_COOLDOWN
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return;

                Long lastCast = castingPhasePlayers.get(player.getUniqueId());
                if (lastCast == null) return;

                if (System.currentTimeMillis() - lastCast < CAST_WINDOW_MS) {
                    try {
                        boolean isRod = false;
                        boolean isHook = false;
                        for (int i = 0; i < event.getPacket().getModifier().size(); i++) {
                            Object val = event.getPacket().getModifier().read(i);
                            if (val != null) {
                                String str = val.toString().toLowerCase();
                                if (str.contains("grappling-hook")) isHook = true;
                                if (str.contains("fishing_rod")) isRod = true;
                            }
                        }
                        
                        // Rule: ALWAYS allow our custom grappling-hook group to bypass suppression.
                        // Only block the generic fishing_rod cooldown that other plugins/game send on cast.
                        if (isHook) return; 

                        if (isRod) {
                            event.setCancelled(true);
                        } else {
                            // Fallback for duration > 0 packets during the tiny cast window 
                            // that don't have a clear group name (likely material packets).
                            int duration = event.getPacket().getIntegers().readSafely(0);
                            if (duration > 0) event.setCancelled(true);
                        }
                    } catch (Exception e) {
                        // Safe fallback: cancel all cooldowns for the player during the tiny cast window
                        event.setCancelled(true);
                    }
                } else {
                    castingPhasePlayers.remove(player.getUniqueId());
                }
            }
        });
    }

    /**
     * Marks a player as being in the initial cast phase of a fishing rod.
     */
    public static void markCast(Player player) {
        castingPhasePlayers.put(player.getUniqueId(), System.currentTimeMillis());
    }
}
