package dev.agam.skyblockitems.api;

import org.bukkit.inventory.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A classloader-safe registry for custom anvil logic.
 * Uses Object results to avoid ClassCastExceptions between plugins.
 */
public class AnvilRegistry {
    
    // Each consumer takes an Object array: [ItemStack left, ItemStack right, Object[] resultContainer]
    // index 0 of resultContainer: ItemStack result
    // index 1 of resultContainer: Integer cost
    private static final List<Consumer<Object[]>> handlers = new ArrayList<>();

    public static void registerHandler(Consumer<Object[]> handler) {
        handlers.add(handler);
    }

    public static List<Consumer<Object[]>> getHandlers() {
        return new ArrayList<>(handlers);
    }
}
