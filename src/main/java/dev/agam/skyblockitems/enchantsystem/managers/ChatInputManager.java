package dev.agam.skyblockitems.enchantsystem.managers;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages chat input for GUI interactions.
 */
public class ChatInputManager {

    private final Map<Player, Consumer<String>> pendingInput = new HashMap<>();

    public void awaitInput(Player player, Consumer<String> callback) {
        pendingInput.put(player, callback);
    }

    public boolean hasAwaiting(Player player) {
        return pendingInput.containsKey(player);
    }

    public void handleInput(Player player, String message) {
        Consumer<String> callback = pendingInput.remove(player);
        if (callback != null) {
            callback.accept(message);
        }
    }

    public void cancel(Player player) {
        pendingInput.remove(player);
    }
}
