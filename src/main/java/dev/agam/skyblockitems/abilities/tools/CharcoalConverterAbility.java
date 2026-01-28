package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CharcoalConverterAbility extends SkyBlockAbility {

    public CharcoalConverterAbility() {
        super("CHARCOAL_CONVERTER", "ממיר פחם עץ", TriggerType.ON_BLOCK_BREAK, 0.0, 0.0, 100.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double chance, double range) {
        if (!(event instanceof BlockBreakEvent))
            return false;
        BlockBreakEvent e = (BlockBreakEvent) event;

        Material type = e.getBlock().getType();
        if (!isLog(type))
            return false;

        // Auto-smelt logic
        e.setDropItems(false);
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.CHARCOAL));

        return true;
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.contains("LOG") || name.contains("WOOD") || name.contains("STEM");
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
