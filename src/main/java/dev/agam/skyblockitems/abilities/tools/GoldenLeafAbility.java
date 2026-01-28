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

public class GoldenLeafAbility extends SkyBlockAbility {

    public GoldenLeafAbility() {
        super("GOLDEN_LEAF", "עלה מוזהב", TriggerType.ON_BLOCK_BREAK, 0.0, 0.0, 5.0, 1.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double chance,
            double amount) {
        if (!(event instanceof BlockBreakEvent))
            return false;
        BlockBreakEvent e = (BlockBreakEvent) event;

        if (!e.getBlock().getType().name().contains("LEAVES"))
            return false;

        double prob = chance / 100.0;
        if (new Random().nextDouble() > prob)
            return false;

        // Drop Golden Apple
        ItemStack apples = new ItemStack(Material.GOLDEN_APPLE, (int) Math.max(1, amount));
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), apples);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
