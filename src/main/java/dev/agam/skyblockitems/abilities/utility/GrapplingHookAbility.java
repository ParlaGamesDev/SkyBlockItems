package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.*;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Grappling Hook ability - Hypixel SkyBlock style
 * Works like a fishing rod: cast once to launch, retract to pull yourself
 */
public class GrapplingHookAbility extends SkyBlockAbility implements Listener {

    // Track hooks that are active grappling hooks
    private final Set<UUID> activeHooks = new HashSet<>();

    public GrapplingHookAbility() {
        super("GRAPPLING_HOOK", "וו תפיסה", TriggerType.RIGHT_CLICK, 3.0, 0.0, 0.0, 0.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // This is handled via PlayerFishEvent
        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.FISHING_ROD)
            return;

        NBTItem nbtItem = NBTItem.get(item);
        if (nbtItem == null || !nbtItem.hasTag("SKYBLOCK_GRAPPLING_HOOK"))
            return;

        // Parse cooldown from NBT
        double cooldown = 3.0;
        String value = nbtItem.getString("SKYBLOCK_GRAPPLING_HOOK");
        if (value != null && !value.isEmpty()) {
            try {
                String[] params = value.trim().split("\\s+");
                if (params.length >= 1) {
                    cooldown = Double.parseDouble(params[0]);
                }
            } catch (Exception ignored) {
            }
        }

        PlayerFishEvent.State state = event.getState();

        if (state == PlayerFishEvent.State.FISHING) {
            // Player cast the hook - mark as active
            activeHooks.add(player.getUniqueId());

            // Send launch message
            String msg = SkyBlockItems.getInstance().getMessagesConfig()
                    .getString("players.grappling-hook-launch", "&e🪝 השלכת את הוו!");
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);

            // Play sound
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.2f);

        } else if (state == PlayerFishEvent.State.REEL_IN || state == PlayerFishEvent.State.IN_GROUND) {
            // Player retracting - check if it was an active grappling hook
            if (!activeHooks.contains(player.getUniqueId()))
                return;

            activeHooks.remove(player.getUniqueId());

            // Check cooldown
            if (dev.agam.skyblockitems.abilities.CooldownManager.isOnCooldown(player.getUniqueId(), "GRAPPLING_HOOK")) {
                double remaining = dev.agam.skyblockitems.abilities.CooldownManager
                        .getRemainingCooldown(player.getUniqueId(), "GRAPPLING_HOOK");
                String msg = SkyBlockItems.getInstance().getMessagesConfig()
                        .getString("players.cooldown", "&cהאביליטי {ability} בטעינה: {remaining} ש'");
                msg = msg.replace("{ability}", getDisplayName())
                        .replace("{remaining}", String.format("%.1f", remaining));
                dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
                return;
            }

            FishHook hook = event.getHook();
            if (hook == null)
                return;

            Location hookLoc = hook.getLocation();
            Location playerLoc = player.getLocation();

            // Calculate pull vector
            Vector direction = hookLoc.toVector().subtract(playerLoc.toVector());
            double distance = direction.length();

            if (distance < 2.0) {
                // Too close, don't pull
                return;
            }

            // Normalize and apply velocity
            // The further the hook, the stronger the pull (capped)
            double pullStrength = Math.min(distance / 10.0, 2.5);
            Vector velocity = direction.normalize().multiply(pullStrength);

            // Add some upward momentum for arc effect
            velocity.setY(velocity.getY() + 0.5);

            player.setVelocity(velocity);

            // Set cooldown
            dev.agam.skyblockitems.abilities.CooldownManager.setCooldown(player.getUniqueId(), "GRAPPLING_HOOK",
                    cooldown);

            // Effects
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 0.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.5f, 1.5f);

            // Particle trail
            final double finalDistance = distance;
            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks > 10 || player.isOnGround()) {
                        cancel();
                        return;
                    }
                    player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 3,
                            new Particle.DustOptions(Color.GRAY, 0.8f));
                    ticks++;
                }
            }.runTaskTimer(SkyBlockItems.getInstance(), 0L, 2L);

            // Negate fall damage for a short duration
            player.setFallDistance(0);
            player.setMetadata("NEGATE_FALL_DAMAGE",
                    new org.bukkit.metadata.FixedMetadataValue(SkyBlockItems.getInstance(), true));

            // Remove the metadata after 5 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.hasMetadata("NEGATE_FALL_DAMAGE")) {
                        player.removeMetadata("NEGATE_FALL_DAMAGE", SkyBlockItems.getInstance());
                    }
                }
            }.runTaskLater(SkyBlockItems.getInstance(), 100L);

            // Send retract message
            String msg = SkyBlockItems.getInstance().getMessagesConfig()
                    .getString("players.grappling-hook-retract", "&a🪝 נמשכת לכיוון הוו!");
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);

        } else if (state == PlayerFishEvent.State.FAILED_ATTEMPT) {
            // Hook missed or failed
            activeHooks.remove(player.getUniqueId());
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
