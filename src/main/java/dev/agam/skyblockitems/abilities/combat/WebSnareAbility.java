package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import dev.agam.skyblockitems.integration.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.ArrayList;
import java.util.List;

public class WebSnareAbility extends SkyBlockAbility {

    public WebSnareAbility() {
        super("WEB_SNARE", "מלכודת קורי עכביש", TriggerType.ON_ARROW_HIT, 10.0, 20.0, 5.0, 0.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double duration,
            double range) {
        if (!(event instanceof ProjectileHitEvent))
            return false;
        ProjectileHitEvent e = (ProjectileHitEvent) event;

        Block hitBlock = e.getHitBlock();
        if (hitBlock == null && e.getHitEntity() != null && !dev.agam.skyblockitems.utils.TargetUtils.isNPC(e.getHitEntity())) {
            hitBlock = e.getHitEntity().getLocation().getBlock();
        }

        if (hitBlock == null)
            return false;

        // Create multiple webs around the impact point
        Block center = hitBlock.getType().isAir() ? hitBlock : hitBlock.getRelative(0, 1, 0);

        java.util.List<Block> webBlocks = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();

        // Add 3-5 webs randomly around the center
        int webCount = 3 + random.nextInt(3); // 3 to 5 webs

        for (int i = 0; i < webCount; i++) {
            int offsetX = random.nextInt(3) - 1; // -1 to 1
            int offsetY = random.nextInt(2); // 0 to 1
            int offsetZ = random.nextInt(3) - 1; // -1 to 1

            Block target = center.getRelative(offsetX, offsetY, offsetZ);
            if (target.getType() == Material.AIR
                    && WorldGuardHook.isAbilitiesEnabled(player, target.getLocation())) {
                webBlocks.add(target);
                target.setType(Material.COBWEB);
            }
        }

        if (center.getType() == Material.AIR
                && WorldGuardHook.isAbilitiesEnabled(player, center.getLocation())) {
            webBlocks.add(center);
            center.setType(Material.COBWEB);
        }

        if (webBlocks.isEmpty())
            return false;

        // Remove after duration (seconds)
        int ticks = (int) (duration * 20);
        if (ticks <= 0)
            ticks = 100;

        final int finalTicks = ticks;
        Bukkit.getScheduler().runTaskLater(SkyBlockItems.getInstance(), () -> {
            for (Block web : webBlocks) {
                if (web.getType() == Material.COBWEB) {
                    web.setType(Material.AIR);
                }
            }
        }, finalTicks);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
