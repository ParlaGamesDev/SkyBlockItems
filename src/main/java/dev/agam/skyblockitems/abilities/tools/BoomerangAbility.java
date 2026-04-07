package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BoomerangAbility extends SkyBlockAbility implements Listener {

    private static final Map<UUID, Set<BoomerangTask>> activeTasks = new HashMap<>();

    public BoomerangAbility() {
        super("BOOMERANG", "בומרנג", TriggerType.RIGHT_CLICK, 5.0, 15.0, 15.0, 10.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        double maxDistance = damage; // params[2] = Distance
        double dmg = range; // params[3] = Damage
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType().isAir())
            return false;

        ItemStack item = heldItem.clone();
        int slot = player.getInventory().getHeldItemSlot();

        // Remove from inventory
        player.getInventory().setItem(slot, null);

        // Spawn ArmorStand
        ArmorStand as = player.getWorld().spawn(player.getEyeLocation().subtract(0, 0.7, 0), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setSmall(true);
            s.setMarker(true);
            s.getEquipment().setHelmet(item);
        });

        Vector direction = player.getLocation().getDirection().normalize();
        Set<UUID> hitEntities = new HashSet<>();

        BoomerangTask task = new BoomerangTask(player, as, item, slot, maxDistance, dmg, direction, hitEntities);
        activeTasks.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(task);
        task.runTaskTimer(SkyBlockItems.getInstance(), 0L, 1L);

        return true;
    }

    private class BoomerangTask extends BukkitRunnable {
        private final Player player;
        private final ArmorStand as;
        private final ItemStack item;
        private final int slot;
        private final double maxDistance;
        private final double dmg;
        private final Vector direction;
        private final Set<UUID> hitEntities;

        private int ticks = 0;
        private boolean returning = false;

        public BoomerangTask(Player player, ArmorStand as, ItemStack item, int slot, double maxDistance, double dmg,
                Vector direction, Set<UUID> hitEntities) {
            this.player = player;
            this.as = as;
            this.item = item;
            this.slot = slot;
            this.maxDistance = maxDistance;
            this.dmg = dmg;
            this.direction = direction;
            this.hitEntities = hitEntities;
        }

        @Override
        public void run() {
            if (!player.isOnline() || as.isDead() || ticks > 300) { // Safety limit 15s
                cleanup();
                return;
            }

            as.getWorld().spawnParticle(Particle.SNOWFLAKE, as.getLocation().add(0, 1.5, 0), 1, 0, 0, 0, 0.02);
            as.setHeadPose(as.getHeadPose().add(0, 0.6, 0));

            Location current = as.getLocation();
            double distanceToPlayer = current.distance(player.getEyeLocation());

            if (!returning) {
                as.teleport(current.add(direction.clone().multiply(0.8)));
                // Check if distance limit reached (as defined in GUI/Config)
                if (ticks * 0.8 >= maxDistance || !as.getLocation().getBlock().getType().isAir()) {
                    returning = true;
                }
            } else {
                Vector returnDir = player.getEyeLocation().subtract(as.getEyeLocation()).toVector().normalize();
                as.teleport(current.add(returnDir.multiply(0.8)));
                if (distanceToPlayer < 1.5) {
                    cleanup();
                    return;
                }
            }

            for (Entity e : as.getNearbyEntities(0.8, 0.8, 0.8)) {
                if (e instanceof LivingEntity && e != player && !hitEntities.contains(e.getUniqueId()) && !dev.agam.skyblockitems.utils.TargetUtils.isNPC(e)) {
                    ((LivingEntity) e).damage(dmg, player);
                    hitEntities.add(e.getUniqueId());
                    e.getWorld().spawnParticle(Particle.FLASH, e.getLocation().add(0, 1, 0), 1);
                    e.getWorld().playSound(e.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.2f);
                    returning = true;
                }
            }
            ticks++;
        }

        public void cleanup() {
            if (as != null)
                as.remove();
            if (player.isOnline()) {
                if (player.getInventory().getItem(slot) == null) {
                    player.getInventory().setItem(slot, item);
                } else {
                    Map<Integer, ItemStack> remaining = player.getInventory().addItem(item);
                    if (!remaining.isEmpty()) {
                        for (ItemStack leftover : remaining.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                        }
                    }
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } else {
                as.getWorld().dropItemNaturally(as.getLocation(), item);
            }

            Set<BoomerangTask> tasks = activeTasks.get(player.getUniqueId());
            if (tasks != null) {
                tasks.remove(this);
                if (tasks.isEmpty())
                    activeTasks.remove(player.getUniqueId());
            }
            cancel();
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Set<BoomerangTask> tasks = activeTasks.get(event.getPlayer().getUniqueId());
        if (tasks != null) {
            new HashSet<>(tasks).forEach(BoomerangTask::cleanup);
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
