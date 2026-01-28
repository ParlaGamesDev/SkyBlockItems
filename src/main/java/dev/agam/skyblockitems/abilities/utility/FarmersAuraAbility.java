package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

public class FarmersAuraAbility extends SkyBlockAbility {

    private final Material cropType;

    public FarmersAuraAbility(String id, String displayName, Material cropType) {
        super(id, displayName, TriggerType.ON_SNEAK, 5.0, 20.0, 10.0, 0.0);
        this.cropType = cropType;
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double radius, double range) {
        int r = (int) radius;
        boolean found = false;

        for (int x = -r; x <= r; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    Block b = player.getLocation().add(x, y, z).getBlock();
                    if (b.getType() == cropType) {
                        if (b.getBlockData() instanceof Ageable) {
                            Ageable ageable = (Ageable) b.getBlockData();
                            if (ageable.getAge() < ageable.getMaximumAge()) {
                                ageable.setAge(ageable.getMaximumAge());
                                b.setBlockData(ageable);
                                found = true;
                            }
                        }
                    }
                }
            }
        }

        if (found) {
            player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20,
                    0.5, 0.5, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 1.0f);
        }

        return found;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
