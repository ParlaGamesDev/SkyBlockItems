package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SkySmashAbility extends SkyBlockAbility {

    private static final Set<UUID> fallingPlayers = new HashSet<>();

    public SkySmashAbility() {
        super("SKY_SMASH", "מכה לשמיים", TriggerType.SHIFT_RIGHT_CLICK, 12.0, 40.0, 5.0, 15.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // height = damage (params[2]), damageValue = range (params[3])
        // To get radius, we parse NBT
        NBTItem nbtItem = NBTItem.get(player.getInventory().getItemInMainHand());
        String val = nbtItem.getString("SKYBLOCK_SKY_SMASH");
        double radius = 5.0;
        try {
            String[] p = val.split("\\s+");
            if (p.length >= 5)
                radius = Double.parseDouble(p[4]);
        } catch (Exception ignored) {
        }

        final double finalRadius = radius;
        final double finalDamage = range;

        // Launch UP
        player.setVelocity(new Vector(0, damage / 4.0, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.2, 0.2, 0.1);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Slam DOWN
                player.setVelocity(new Vector(0, -2, 0));
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.5f);
                fallingPlayers.add(player.getUniqueId());

                // Wait for land
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnGround() || player.getLocation().getBlock().getType() != Material.AIR) {
                            executeSlam(player, finalRadius, finalDamage);
                            fallingPlayers.remove(player.getUniqueId());
                            cancel();
                        }
                    }
                }.runTaskTimer(SkyBlockItems.getInstance(), 1L, 1L);
            }
        }.runTaskLater(SkyBlockItems.getInstance(), 10L); // Slam after 0.5s

        return true;
    }

    private void executeSlam(Player player, double radius, double damage) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);

        // Ground Smash Particles
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            for (double r = 1; r <= radius; r += 1.5) {
                double x = Math.cos(rad) * r;
                double z = Math.sin(rad) * r;
                player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(x, 0.1, z), 3, 0.1, 0.1, 0.1,
                        0.05, Material.DIRT.createBlockData());
            }
        }
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);

        // Damage nearby
        for (Entity e : player.getNearbyEntities(radius, 3.0, radius)) {
            if (e instanceof LivingEntity && e != player) {
                ((LivingEntity) e).damage(damage, player);
                e.setVelocity(new Vector(0, 0.5, 0)); // Pop them up a bit
            }
        }
    }

    public static boolean isFalling(UUID uuid) {
        return fallingPlayers.contains(uuid);
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
