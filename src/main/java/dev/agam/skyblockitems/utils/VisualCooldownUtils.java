package dev.agam.skyblockitems.utils;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Visual Cooldown Utility.
 */
public class VisualCooldownUtils {

    private static final Map<UUID, Map<String, Long>> activeCooldownWindows = new ConcurrentHashMap<>();

    public static boolean hasActiveCooldown(UUID uuid, String abilityId) {
        Map<String, Long> windows = activeCooldownWindows.get(uuid);
        if (windows == null) return false;
        Long end = windows.get(abilityId);
        if (end == null) return false;
        if (System.currentTimeMillis() > end) {
            windows.remove(abilityId);
            return false;
        }
        return true;
    }

    public static void setCooldownActive(UUID uuid, String abilityId, int ticks) {
        activeCooldownWindows.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(abilityId, System.currentTimeMillis() + (ticks * 50L));
    }

    public static void resetVisualCooldown(Player player, String abilityId) {
        if (player == null || abilityId == null || abilityId.isEmpty()) return;
        try {
            NamespacedKey groupKey = new NamespacedKey("skyblockitems", abilityId.toLowerCase().replace("_", "-"));
            Object nmsPacket = createCooldownPacket(groupKey, 0); // Duration 0 forces reset
            if (nmsPacket != null) {
                PacketContainer container = PacketContainer.fromPacket(nmsPacket);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
            }
        } catch (Throwable ignored) {}
    }

    public static void sendVisualCooldown(Player player, Material material, int ticks, String abilityId) {
        if (!SkyBlockItems.getInstance().getConfig().getBoolean("abilities.visual-cooldown.enabled", true)) {
            return;
        }
        if (player == null || ticks <= 0) return;

        if (abilityId == null || abilityId.isEmpty()) {
            player.setCooldown(material, ticks);
            return;
        }

        try {
            NamespacedKey groupKey = new NamespacedKey("skyblockitems", abilityId.toLowerCase().replace("_", "-"));
            String nbtTag = "SKYBLOCK_" + abilityId.toUpperCase().replace("-", "_");
            
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) continue;
                // 1.21.2+: use_cooldown on FISHING_ROD makes the client play the sweep on every
                // right-click (cast + reel) before the server can react. Cooldown group is applied
                // via ClientboundCooldownPacket only — do not put the component on the rod stack.
                if (item.getType() == Material.FISHING_ROD) continue;
                NBTItem nbt = NBTItem.get(item);
                if (nbt.hasTag(nbtTag)) {
                    injectCooldownComponent(item, groupKey, ticks);
                }
            }

            Object nmsPacket = createCooldownPacket(groupKey, ticks);
            if (nmsPacket != null) {
                setCooldownActive(player.getUniqueId(), abilityId, ticks);
                PacketContainer container = PacketContainer.fromPacket(nmsPacket);
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, container);
            } else {
                player.setCooldown(material, ticks);
            }
        } catch (Throwable t) {
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
            Method parseMethod = rlClass.getMethod("parse", String.class);
            Object resourceLocation = parseMethod.invoke(null, groupKey.toString());
            Constructor<?> constructor = packetClass.getConstructor(rlClass, int.class);
            return constructor.newInstance(resourceLocation, duration);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearCooldownComponent(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        try {
            Class<?> dctClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            Object type = dctClass.getField("USE_COOLDOWN").get(null);
            
            for (Method m : item.getClass().getMethods()) {
                if ((m.getName().equals("set") || m.getName().equals("setData")) && m.getParameterCount() == 2) {
                    m.setAccessible(true);
                    m.invoke(item, type, null);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    private static boolean injectCooldownComponent(ItemStack item, NamespacedKey groupKey, int ticks) {
        try {
            Class<?> dctClass = Class.forName("io.papermc.paper.datacomponent.DataComponentTypes");
            Object type = dctClass.getField("USE_COOLDOWN").get(null);
            Class<?> ucInterface = Class.forName("io.papermc.paper.datacomponent.item.UseCooldown");
            Method buildStatic = ucInterface.getMethod("useCooldown", float.class);
            Object builder = buildStatic.invoke(null, ticks / 20f);
            
            Method groupMethod = null;
            for (Method m : builder.getClass().getMethods()) {
                if (m.getName().equals("cooldownGroup") && m.getParameterCount() == 1) {
                    groupMethod = m;
                    break;
                }
            }
            if (groupMethod == null) return false;
            groupMethod.setAccessible(true);
            builder = groupMethod.invoke(builder, groupKey);
            Method buildMethod = builder.getClass().getMethod("build");
            buildMethod.setAccessible(true);
            Object component = buildMethod.invoke(builder);
            for (Method m : item.getClass().getMethods()) {
                if ((m.getName().equals("set") || m.getName().equals("setData")) && m.getParameterCount() == 2) {
                    m.setAccessible(true);
                    m.invoke(item, type, component);
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }
}
