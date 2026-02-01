package dev.agam.skyblockitems.utils;

import dev.agam.skyblockitems.SkyBlockItems;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class MessageUtils {

    public enum MessageType {
        CHAT,
        ACTION_BAR
    }

    /**
     * Sends a message to the player based on the global routing setting in
     * abilities.yml.
     */
    public static void sendMessage(Player player, String message) {
        if (message == null || message.isEmpty())
            return;

        String routingStr = SkyBlockItems.getInstance().getConfig().getString("message-routing", "ACTION_BAR");
        MessageType type;
        try {
            type = MessageType.valueOf(routingStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = MessageType.ACTION_BAR;
        }

        send(player, message, type);
    }

    /**
     * Sends a message to the player using a specific type, ignoring global
     * settings.
     */
    public static void send(Player player, String message, MessageType type) {
        if (message == null || message.isEmpty())
            return;

        String translated = ColorUtils.translate(message);

        if (type == MessageType.ACTION_BAR) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(translated));
        } else {
            player.sendMessage(translated);
        }
    }
}
