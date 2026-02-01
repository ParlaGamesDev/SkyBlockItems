package dev.agam.skyblockitems.integration;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Global combat protection listener.
 * Blocks ALL damage and knockback to protected entities based on config.
 */
public class MMOItemsAbilityListener implements Listener {

    // Track recently damaged players to block velocity changes
    private static final Set<UUID> recentlyDamaged = new HashSet<>();

    /**
     * LOWEST priority ensures we run FIRST before any other plugin.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = getPlayerAttacker(event);
        if (attacker == null)
            return;

        Entity victim = event.getEntity();

        // PvP Protection
        if (victim instanceof Player && victim != attacker) {
            if (isPvPDisabled()) {
                event.setCancelled(true);
                event.setDamage(0);
                // Mark player as recently attacked to block velocity
                markForVelocityBlock((Player) victim);
                return;
            }
        }

        // NPC Protection
        if (isBeautyNpc(victim)) {
            if (isNPCProtectionEnabled()) {
                event.setCancelled(true);
                event.setDamage(0);
                return;
            }
        }
    }

    /**
     * Catch ALL damage events (including custom/magic)
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (event instanceof EntityDamageByEntityEvent)
            return;

        Player victim = (Player) event.getEntity();

        if (isPvPDisabled()) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            // Block magic, custom, and explosion damage
            if (cause == EntityDamageEvent.DamageCause.MAGIC ||
                    cause == EntityDamageEvent.DamageCause.CUSTOM ||
                    cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                    cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    cause == EntityDamageEvent.DamageCause.LIGHTNING ||
                    cause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                    cause == EntityDamageEvent.DamageCause.POISON ||
                    cause == EntityDamageEvent.DamageCause.WITHER) {
                event.setCancelled(true);
                event.setDamage(0);
                markForVelocityBlock(victim);
            }
        }
    }

    /**
     * Block velocity changes (knockback) for protected players.
     * This catches knockback from abilities that apply velocity directly.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (!isPvPDisabled())
            return;

        Player player = event.getPlayer();

        // Block velocity if player was recently marked
        if (recentlyDamaged.contains(player.getUniqueId())) {
            // Only block horizontal velocity (keep Y for jumping)
            Vector current = event.getVelocity();
            if (Math.abs(current.getX()) > 0.1 || Math.abs(current.getZ()) > 0.1) {
                event.setCancelled(true);
            }
            recentlyDamaged.remove(player.getUniqueId());
        }
    }

    /**
     * Mark a player for velocity blocking in the next tick.
     */
    private void markForVelocityBlock(Player player) {
        recentlyDamaged.add(player.getUniqueId());
        // Remove after 5 ticks to avoid memory leak
        Bukkit.getScheduler().runTaskLater(
                dev.agam.skyblockitems.SkyBlockItems.getInstance(),
                () -> recentlyDamaged.remove(player.getUniqueId()),
                5L);
    }

    private boolean isPvPDisabled() {
        return dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfig()
                .getBoolean("disable-abilities-on-players", true);
    }

    private boolean isNPCProtectionEnabled() {
        return dev.agam.skyblockitems.SkyBlockItems.getInstance().getConfig()
                .getBoolean("disable-abilities-on-npcs", true);
    }

    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();

        if (damager instanceof Player) {
            return (Player) damager;
        }

        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) {
                return (Player) proj.getShooter();
            }
        }

        if (damager instanceof org.bukkit.entity.AreaEffectCloud) {
            org.bukkit.entity.AreaEffectCloud cloud = (org.bukkit.entity.AreaEffectCloud) damager;
            if (cloud.getSource() instanceof Player) {
                return (Player) cloud.getSource();
            }
        }

        if (damager.hasMetadata("shooter")) {
            if (!damager.getMetadata("shooter").isEmpty()) {
                Object val = damager.getMetadata("shooter").get(0).value();
                if (val instanceof Player) {
                    return (Player) val;
                }
            }
        }

        return null;
    }

    private boolean isBeautyNpc(Entity entity) {
        if (entity == null)
            return false;

        if (entity.hasMetadata("NPC"))
            return true;
        if (entity.hasMetadata("typewriter"))
            return true;

        String className = entity.getClass().getName();
        if (className.contains("Citizens") || className.contains("NPC"))
            return true;

        if (entity instanceof ArmorStand)
            return true;

        return false;
    }
}
