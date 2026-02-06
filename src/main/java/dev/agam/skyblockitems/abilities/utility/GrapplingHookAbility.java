package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Grappling Hook ability - Bow/Crossbow flight edition.
 * Features: Absolute camera and movement lock during flight via event override.
 */
public class GrapplingHookAbility extends SkyBlockAbility implements Listener {

    private static final double COOLDOWN = 5.0;

    // Track active flight sessions
    private final Map<UUID, Arrow> activeArrows = new HashMap<>();
    private final Map<UUID, float[]> lockedRotations = new HashMap<>();

    public GrapplingHookAbility() {
        super("GRAPPLING_HOOK", "וו תפיסה", TriggerType.ON_ARROW_HIT, COOLDOWN, 0.0, 0.0, 0.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (!(event.getProjectile() instanceof Arrow))
            return;

        ItemStack bow = event.getBow();
        if (bow == null || (bow.getType() != Material.BOW && bow.getType() != Material.CROSSBOW))
            return;

        NBTItem nbtItem = NBTItem.get(bow);
        if (nbtItem == null || !nbtItem.hasTag("SKYBLOCK_GRAPPLING_HOOK"))
            return;

        // Check cooldown
        if (dev.agam.skyblockitems.abilities.CooldownManager.isOnCooldown(player.getUniqueId(), "GRAPPLING_HOOK")) {
            double remaining = dev.agam.skyblockitems.abilities.CooldownManager
                    .getRemainingCooldown(player.getUniqueId(), "GRAPPLING_HOOK");
            String msg = SkyBlockItems.getInstance().getConfigManager().getMessage("players.cooldown",
                    "{ability}", getDisplayName(),
                    "{remaining}", String.format("%.1f", remaining));
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
            event.setCancelled(true);
            return;
        }

        Arrow arrow = (Arrow) event.getProjectile();
        activeArrows.put(player.getUniqueId(), arrow);

        // --- STRICT MOVEMENT & CAMERA LOCK ---
        final float originalWalkSpeed = player.getWalkSpeed();
        player.setWalkSpeed(0);

        // Capture initial rotation to lock it
        lockedRotations.put(player.getUniqueId(), new float[] {
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        });

        // Set cooldown immediately
        dev.agam.skyblockitems.abilities.CooldownManager.setCooldown(player.getUniqueId(), "GRAPPLING_HOOK", COOLDOWN);

        // Flight Task
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 160;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || arrow.isDead() || arrow.isOnGround() || !arrow.isValid() || ticks > maxTicks
                        || !activeArrows.containsKey(player.getUniqueId())) {
                    endFlight(player, originalWalkSpeed);
                    cancel();
                    return;
                }

                // Smoothly pull player towards arrow position
                Location target = arrow.getLocation().clone();
                float[] rot = lockedRotations.get(player.getUniqueId());
                if (rot != null) {
                    target.setYaw(rot[0]);
                    target.setPitch(rot[1]);
                }

                // Aggressive teleport to follow arrow exactly but keep locked rotation
                player.teleport(target);
                player.setVelocity(arrow.getVelocity());
                player.setFallDistance(0);

                // Particles
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 1, 0.05, 0.05, 0.05, 0.01);
            }
        }.runTaskTimer(SkyBlockItems.getInstance(), 1L, 1L);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 0.9f);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onCameraMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!lockedRotations.containsKey(player.getUniqueId()))
            return;

        float[] rot = lockedRotations.get(player.getUniqueId());
        if (rot == null)
            return;

        Location to = event.getTo();
        if (to == null)
            return;

        // If client turned the mouse, reset rotation in the 'to' location
        if (to.getYaw() != rot[0] || to.getPitch() != rot[1]) {
            to.setYaw(rot[0]);
            to.setPitch(rot[1]);
            event.setTo(to);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow))
            return;
        Arrow arrow = (Arrow) event.getEntity();
        if (!(arrow.getShooter() instanceof Player))
            return;

        Player player = (Player) arrow.getShooter();
        if (activeArrows.containsKey(player.getUniqueId()) && activeArrows.get(player.getUniqueId()).equals(arrow)) {
            activeArrows.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeArrows.remove(uuid);
        lockedRotations.remove(uuid);
    }

    private void endFlight(Player player, float originalSpeed) {
        if (player.isOnline()) {
            lockedRotations.remove(player.getUniqueId());
            player.setWalkSpeed(originalSpeed <= 0 ? 0.2f : originalSpeed);
            player.setFallDistance(0);

            player.setMetadata("NEGATE_FALL_DAMAGE", new FixedMetadataValue(SkyBlockItems.getInstance(), true));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.hasMetadata("NEGATE_FALL_DAMAGE")) {
                        player.removeMetadata("NEGATE_FALL_DAMAGE", SkyBlockItems.getInstance());
                    }
                }
            }.runTaskLater(SkyBlockItems.getInstance(), 80L);
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
