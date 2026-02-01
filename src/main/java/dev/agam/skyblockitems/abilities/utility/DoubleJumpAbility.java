package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class DoubleJumpAbility extends SkyBlockAbility implements Listener {

    public DoubleJumpAbility() {
        super("DOUBLE_JUMP", "קפיצה כפולה", TriggerType.ON_JUMP, 3.0, 15.0, 0.8, 0.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // Enable flight allow for the player if they have the ability (Passive check)
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            // Only set allow flight if they are NOT already flying (e.g. from Essentials
            // /fly)
            if (!player.isFlying() && !player.getAllowFlight()) {
                player.setAllowFlight(true);
                // Mark that we are the ones who enabled it
                player.setMetadata("SKYBLOCK_FLIGHT_ENABLED",
                        new org.bukkit.metadata.FixedMetadataValue(SkyBlockItems.getInstance(), true));
            }
        }
        return true;
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
            return;

        // If player is ALREADY flying (e.g. from /fly), don't treat double tap as jump
        if (player.isFlying())
            return;

        // Check for ability in hands OR armor
        boolean hasAbility = false;
        ItemStack[] pieces = new ItemStack[] {
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand(),
                player.getInventory().getArmorContents()[0],
                player.getInventory().getArmorContents()[1],
                player.getInventory().getArmorContents()[2],
                player.getInventory().getArmorContents()[3]
        };

        for (ItemStack item : pieces) {
            if (item == null || item.getType().isAir())
                continue;
            if (io.lumine.mythic.lib.api.item.NBTItem.get(item).hasTag("SKYBLOCK_DOUBLE_JUMP")) {
                hasAbility = true;
                break;
            }
        }

        if (!hasAbility) {
            // Only disable flight if WE were the ones who enabled it
            if (player.hasMetadata("SKYBLOCK_FLIGHT_ENABLED")) {
                player.setAllowFlight(false);
                player.removeMetadata("SKYBLOCK_FLIGHT_ENABLED", SkyBlockItems.getInstance());
            }
            return;
        }

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        // Perform the jump - simplified and punchy
        Vector velocity = player.getLocation().getDirection().multiply(1.0).setY(1.0);
        player.setVelocity(velocity);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.2, 0.2, 0.2, 0.1);
    }

    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
            return;

        // Reset flight allow when touching ground - but ONLY if they have the ability
        if (player.isOnGround() && !player.getAllowFlight()) {
            // Check if player actually has the ability before enabling flight
            boolean hasAbility = false;
            ItemStack[] pieces = new ItemStack[] {
                    player.getInventory().getItemInMainHand(),
                    player.getInventory().getItemInOffHand(),
                    player.getInventory().getArmorContents()[0],
                    player.getInventory().getArmorContents()[1],
                    player.getInventory().getArmorContents()[2],
                    player.getInventory().getArmorContents()[3]
            };

            for (ItemStack item : pieces) {
                if (item == null || item.getType().isAir())
                    continue;
                if (io.lumine.mythic.lib.api.item.NBTItem.get(item).hasTag("SKYBLOCK_DOUBLE_JUMP")) {
                    hasAbility = true;
                    break;
                }
            }

            if (hasAbility) {
                // Only set allow flight if NOT already flying
                if (!player.isFlying()) {
                    player.setAllowFlight(true);
                    player.setMetadata("SKYBLOCK_FLIGHT_ENABLED",
                            new org.bukkit.metadata.FixedMetadataValue(SkyBlockItems.getInstance(), true));
                }
            }
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
