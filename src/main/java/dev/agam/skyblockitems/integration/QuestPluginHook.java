package dev.agam.skyblockitems.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Notifies CrazyNPCQuests when the SkyBlockItems crafting GUI produces an item.
 */
public final class QuestPluginHook {

    private static final String API_CLASS = "dev.agam.CrazyNPCQuests.api.QuestCraftAPI";
    private static final String PLUGIN_NAME = "CrazyNPCQuests";

    private QuestPluginHook() {}

    public static void notifyCraft(Player player, ItemStack result, int amount, String recipeMaterial) {
        if (player == null || result == null || result.getType().isAir() || amount <= 0) {
            return;
        }
        Plugin quests = findQuestsPlugin();
        if (quests == null || !quests.isEnabled()) {
            return;
        }
        try {
            Class<?> api = Class.forName(API_CLASS, true, quests.getClass().getClassLoader());
            api.getMethod("onSkyBlockCraft", Player.class, ItemStack.class, int.class, String.class)
                    .invoke(null, player, result.clone(), amount, recipeMaterial);
        } catch (ReflectiveOperationException ignored) {
            // Older CrazyNPCQuests — SkyBlockCraftEvent listener handles it
        }
    }

    private static Plugin findQuestsPlugin() {
        Plugin direct = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (direct != null) {
            return direct;
        }
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if ("dev.agam.CrazyNPCQuests.CrazyNPCQuestsPlugin".equals(plugin.getClass().getName())) {
                return plugin;
            }
        }
        return null;
    }
}
