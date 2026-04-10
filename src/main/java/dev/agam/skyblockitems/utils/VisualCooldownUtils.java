package dev.agam.skyblockitems.utils;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import dev.agam.skyblockitems.SkyBlockItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Advanced Visual Cooldown Utility - Fixed Reflection.
 */
public class VisualCooldownUtils {

    public static void sendVisualCooldown(Player player, Material material, int ticks, String abilityId) {
        Logger logger = SkyBlockItems.getInstance().getLogger();
        
        if (!SkyBlockItems.getInstance().getConfig().getBoolean("abilities.visual-cooldown.enabled", true)) {
            return;
        }

        if (player == null || ticks <= 0) return;

        logger.info("[VisualCooldown] Triggered for " + player.getName() + " - Ability: " + abilityId + " (" + ticks + " ticks)");

        if (abilityId == null || abilityId.isEmpty()) {
            player.setCooldown(material, ticks);
            return;
        }

        try {
            NamespacedKey groupKey = new NamespacedKey("skyblockitems", abilityId.toLowerCase().replace("_", "-"));
            
            // 2. Inject the 'use_cooldown' component
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held != null && !held.getType().isAir()) {
                if (injectCooldownComponent(held, groupKey, ticks)) {
                    logger.info("[VisualCooldown] Component injection success.");
                } else {
                    logger.warning("[VisualCooldown] Component injection failed, falling back to material.");
                    player.setCooldown(material, ticks);
                    return;
                }
            } else {
                player.setCooldown(material, ticks);
                return;
            }

            // 3. Construct and send the packet
            Object nmsPacket = createCooldownPacket(groupKey, ticks);
            if (nmsPacket != null) {
                PacketContainer container = PacketContainer.fromPacket(nmsPacket);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
                logger.info("[VisualCooldown] Isolated packet sent successfully.");
            } else {
                player.setCooldown(material, ticks);
            }

        } catch (Throwable t) {
            logger.severe("[VisualCooldown] Error: " + t.getMessage());
            player.setCooldown(material, ticks);
        }
    }

    public static void sendVisualCooldown(Player player, Material material, int ticks) {
        sendVisualCooldown(player, material, ticks, null);
    }

    private static Object createCooldownPacket(NamespacedKey groupKey, int duration) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundCooldownPacket");
            Class<?> rlClass = Class.forName("net.minecraft.resources.ResourceLocation");

            // Use parse method (works on 1.21.x)
            Method parseMethod = rlClass.getMethod("parse", String.class);
            Object resourceLocation = parseMethod.invoke(null, groupKey.toString());

            Constructor<?> constructor = packetClass.getConstructor(rlClass, int.class);
            return constructor.newInstance(resourceLocation, duration);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean injectCooldownComponent(ItemStack item, NamespacedKey groupKey, int ticks) {
        try {
            // Get DataComponentType for USE_COOLDOWN
            Class<?> dctClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            Object type = dctClass.getField("USE_COOLDOWN").get(null);

            // Access UseCooldown.Builder via its interface
            Class<?> ucInterface = Class.forName("io.papermc.paper.datacomponent.item.UseCooldown");
            Method buildStatic = ucInterface.getMethod("useCooldown", float.class);
            Object builder = buildStatic.invoke(null, ticks / 20f);
            
            // Find cooldownGroup method on the builder (trying multiple ways)
            Method groupMethod = null;
            for (Method m : builder.getClass().getMethods()) {
                if (m.getName().equals("cooldownGroup") && m.getParameterCount() == 1) {
                    groupMethod = m;
                    break;
                }
            }
            
            if (groupMethod == null) throw new NoSuchMethodException("cooldownGroup not found on builder");
            
            groupMethod.setAccessible(true);
            builder = groupMethod.invoke(builder, groupKey);
            
            Method buildMethod = builder.getClass().getMethod("build");
            buildMethod.setAccessible(true);
            Object component = buildMethod.invoke(builder);

            // Set the component on the ItemStack using modern 'set' or 'setData'
            Method setMethod = null;
            for (Method m : item.getClass().getMethods()) {
                if ((m.getName().equals("set") || m.getName().equals("setData")) && m.getParameterCount() == 2) {
                    setMethod = m;
                    break;
                }
            }

            if (setMethod != null) {
                setMethod.setAccessible(true);
                setMethod.invoke(item, type, component);
                return true;
            }

        } catch (Exception e) {
            SkyBlockItems.getInstance().getLogger().warning("[VisualCooldown] Injection error: " + e.getMessage());
        }
        return false;
    }
}
