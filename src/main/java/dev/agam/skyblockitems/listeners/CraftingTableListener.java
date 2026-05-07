package dev.agam.skyblockitems.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.crafting.gui.CraftingGUI;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CraftingTableListener implements Listener {

    private final SkyBlockItems plugin;

    public CraftingTableListener(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CRAFTING_TABLE) return;

        event.setCancelled(true);
        new CraftingGUI(plugin, event.getPlayer()).open();
    }
}
