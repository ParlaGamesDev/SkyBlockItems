package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LuckyTreasureAbility extends SkyBlockAbility {

    private final Random random = new Random();

    public LuckyTreasureAbility() {
        super("LUCKY_TREASURE", "אוצר מזל", TriggerType.ON_BLOCK_BREAK, 0.0, 0.0, 10.0, 2.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        if (!(event instanceof BlockBreakEvent))
            return false;
        BlockBreakEvent e = (BlockBreakEvent) event;

        // chance = damage (params[2]), multiplier = range (params[3])
        double chance = damage / 100.0;
        int multiplier = (int) range;
        if (multiplier < 1)
            multiplier = 1;

        if (e.getBlock().hasMetadata("PLACED_BY_PLAYER"))
            return false;

        if (random.nextDouble() > chance)
            return false;

        // Drop extra loot
        // We subtract 1 from the multiplier because one drop already happens naturally
        int extraCount = multiplier - 1;
        if (extraCount <= 0)
            return false;

        for (ItemStack drop : e.getBlock().getDrops(player.getInventory().getItemInMainHand(), player)) {
            ItemStack extra = drop.clone();
            extra.setAmount(extra.getAmount() * extraCount);
            if (extra.getAmount() > 0) {
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), extra);
            }
        }

        // Visual and Sound effects
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, e.getBlock().getLocation().add(0.5, 0.5, 0.5), 15, 0.3,
                0.3, 0.3, 0.1);
        player.getWorld().playSound(e.getBlock().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
