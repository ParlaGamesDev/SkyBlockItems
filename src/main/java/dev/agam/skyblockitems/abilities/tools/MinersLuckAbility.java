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
import java.util.Random;

public class MinersLuckAbility extends SkyBlockAbility {

    public MinersLuckAbility() {
        super("MINERS_LUCK", "מזל של כורים", TriggerType.ON_BLOCK_BREAK, 0.0, 0.0, 10.0, 1.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double chance,
            double amount) {
        if (!(event instanceof BlockBreakEvent))
            return false;
        BlockBreakEvent e = (BlockBreakEvent) event;

        Material type = e.getBlock().getType();
        if (type != Material.STONE && type != Material.DEEPSLATE)
            return false;

        double prob = chance / 100.0;
        if (new Random().nextDouble() > prob)
            return false;

        // Drop Iron Nugget
        ItemStack nuggets = new ItemStack(Material.IRON_NUGGET, (int) Math.max(1, amount));
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), nuggets);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
