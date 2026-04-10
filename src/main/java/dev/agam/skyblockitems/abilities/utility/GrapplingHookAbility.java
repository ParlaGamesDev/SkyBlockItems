package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import dev.agam.skyblockitems.abilities.CooldownManager;
import dev.agam.skyblockitems.utils.MessageUtils;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Grappling Hook ability - Fishing Rod edition.
 * Launches the player towards the hook when reeling in.
 */
public class GrapplingHookAbility extends SkyBlockAbility implements Listener {

    private static final double DEFAULT_COOLDOWN = 2.0;

    public GrapplingHookAbility() {
        super("GRAPPLING_HOOK", "Grappling Hook", TriggerType.RIGHT_CLICK, DEFAULT_COOLDOWN, 0.0, 0.0, 0.0);
        SkyBlockItems.getInstance().getServer().getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // Handled via PlayerFishEvent listener for better fishing rod state detection
        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.FISHING_ROD) return;

        NBTItem nbtItem = NBTItem.get(item);
        if (!nbtItem.hasTag("SKYBLOCK_GRAPPLING_HOOK")) return;

        if (event.getState() == PlayerFishEvent.State.REEL_IN || 
            event.getState() == PlayerFishEvent.State.IN_GROUND ||
            event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            
            // Prevent pulling NPCs
            if (event.getCaught() != null && dev.agam.skyblockitems.utils.TargetUtils.isNPC(event.getCaught())) {
                event.setCancelled(true);
                return;
            }

            FishHook hook = event.getHook();
            
            // WorldGuard Check
            if (!dev.agam.skyblockitems.integration.WorldGuardHook.isGrapplingHookEnabled(player, hook.getLocation())) {
                return;
            }

            // Check Cooldown
            if (CooldownManager.isOnCooldown(player.getUniqueId(), "GRAPPLING_HOOK")) {
                double remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), "GRAPPLING_HOOK");
                String msg = SkyBlockItems.getInstance().getConfigManager().getMessage("players.cooldown",
                        "{ability}", getDisplayName(),
                        "{remaining}", String.format("%.1f", remaining));
                MessageUtils.send(player, msg, MessageUtils.MessageType.CHAT);
                return;
            }

            // Parse cooldown from NBT if available
            double cooldownVal = DEFAULT_COOLDOWN;
            String nbtVal = nbtItem.getString("SKYBLOCK_GRAPPLING_HOOK");
            if (nbtVal != null && !nbtVal.isEmpty()) {
                try {
                    cooldownVal = Double.parseDouble(nbtVal.split("\\s+")[0]);
                } catch (Exception ignored) {}
            }

            // Launch Player
            Vector playerLoc = player.getLocation().toVector();
            Vector hookLoc = hook.getLocation().toVector();
            
            Vector direction = hookLoc.subtract(playerLoc);
            
            // Limit distance to prevent infinite pull if hook glitched
            double distance = direction.length();
            if (distance > 64) distance = 64; 
            
            // Calculate velocity
            // Increased power for snappier feel like Hypixel (Boosted further as requested)
            Vector velocity = direction.clone().normalize().multiply(Math.min(distance * 0.3, 3.5));
            velocity.setY(velocity.getY() * 0.6 + 0.7); // Stronger upward boost
            
            player.setVelocity(velocity);
            
            // Sounds & Feedback
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
            
            // Set Cooldown
            CooldownManager.setCooldown(player.getUniqueId(), "GRAPPLING_HOOK", cooldownVal);
            
            // Negate Fall Damage
            player.setMetadata("NEGATE_FALL_DAMAGE", new FixedMetadataValue(SkyBlockItems.getInstance(), true));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.hasMetadata("NEGATE_FALL_DAMAGE")) {
                        player.removeMetadata("NEGATE_FALL_DAMAGE", SkyBlockItems.getInstance());
                    }
                }
            }.runTaskLater(SkyBlockItems.getInstance(), 100L); // 5 sec protection
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        List<String> lore = new ArrayList<>();
        lore.add(COLOR_GRAY + "משגר אותך לעבר הקרס של החכה!");
        lore.add("");
        lore.add(formatManaAndCooldown(manaCost, cooldown));
        return lore;
    }
}
