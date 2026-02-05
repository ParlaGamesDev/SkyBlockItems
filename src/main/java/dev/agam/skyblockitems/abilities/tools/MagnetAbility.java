package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class MagnetAbility extends SkyBlockAbility {

    public MagnetAbility() {
        super("MAGNET", "מגנט", TriggerType.RIGHT_CLICK, 5.0, 10.0, 0.0, 10.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // radius = damage
        double radius = damage > 0 ? damage : 10.0;
        final Location targetLoc = player.getLocation().clone();

        List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
        List<Item> itemsToMagnet = new ArrayList<>();

        for (Entity e : nearby) {
            if (e instanceof Item) {
                itemsToMagnet.add((Item) e);
            }
        }

        if (itemsToMagnet.isEmpty())
            return false;

        player.getWorld().playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.PORTAL, targetLoc.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 20) { // Magnet duration approx 1 second
                    cancel();
                    return;
                }

                for (Item item : itemsToMagnet) {
                    if (item.isValid() && !item.isDead()) {
                        Vector vec = targetLoc.toVector().subtract(item.getLocation().toVector());
                        double dist = vec.length();
                        if (dist > 0.5) {
                            item.setVelocity(vec.normalize().multiply(0.6));
                            item.getWorld().spawnParticle(Particle.DUST, item.getLocation(), 1,
                                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.PURPLE, 0.5f));
                        }
                    }
                }
                ticks++;
            }
        }.runTaskTimer(SkyBlockItems.getInstance(), 0L, 1L);

        return true;
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
