package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import dev.agam.skyblockitems.abilities.CooldownManager;
import dev.agam.skyblockitems.utils.CooldownPacketSuppressor;
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
 * Grappling Hook ability - Fixed for premature cooldown triggers from other plugins.
 */
public class GrapplingHookAbility extends SkyBlockAbility implements Listener {

    private static final double DEFAULT_COOLDOWN = 2.0;

    public GrapplingHookAbility() {
        super("GRAPPLING_HOOK", "Grappling Hook", TriggerType.RIGHT_CLICK, DEFAULT_COOLDOWN, 0.0, 0.0, 0.0);
        SkyBlockItems.getInstance().getServer().getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() != Material.FISHING_ROD) return;

        NBTItem nbtItem = NBTItem.get(item);
        if (!nbtItem.hasTag("SKYBLOCK_GRAPPLING_HOOK")) return;

        if (event.getState() == PlayerFishEvent.State.FISHING) {
            // 1. Check Cooldown before casting to prevent "Double Use" mess
            if (CooldownManager.isOnCooldown(player.getUniqueId(), "GRAPPLING_HOOK")) {
                double remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), "GRAPPLING_HOOK") ;
                String msg = SkyBlockItems.getInstance().getConfigManager().getMessage("players.cooldown",
                        "{ability}", getDisplayName(),
                        "{remaining}", String.format("%.1f", remaining));
                MessageUtils.send(player, msg, MessageUtils.MessageType.CHAT);
                event.setCancelled(true);
                return;
            }

            // Strip vanilla rod cooldown on cast; suppressor blocks stray SET_COOLDOWN for ~300ms
            // so vanilla/MMOItems cannot flash the sweep before reel-in (1.21.2+ item cooldown model).
            CooldownPacketSuppressor.markCast(player);
            player.setCooldown(Material.FISHING_ROD, 0);
            return;
        }

        if (event.getState() == PlayerFishEvent.State.REEL_IN ||
                event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {

            if (event.getCaught() != null && dev.agam.skyblockitems.utils.TargetUtils.isNPC(event.getCaught())) {
                event.setCancelled(true);
                return;
            }

            FishHook hook = event.getHook();
            if (hook == null) return;

            if (!dev.agam.skyblockitems.integration.WorldGuardHook.isGrapplingHookEnabled(player, hook.getLocation())) {
                return;
            }

            if (CooldownManager.isOnCooldown(player.getUniqueId(), "GRAPPLING_HOOK")) {
                double remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), "GRAPPLING_HOOK");
                String msg = SkyBlockItems.getInstance().getConfigManager().getMessage("players.cooldown",
                        "{ability}", getDisplayName(),
                        "{remaining}", String.format("%.1f", remaining));
                MessageUtils.send(player, msg, MessageUtils.MessageType.CHAT);

                if (event.getHook() != null) {
                    event.getHook().remove();
                }

                event.setCancelled(true);
                return;
            }

            double cooldownVal = DEFAULT_COOLDOWN;
            String nbtVal = nbtItem.getString("SKYBLOCK_GRAPPLING_HOOK");
            if (nbtVal != null && !nbtVal.isEmpty()) {
                try {
                    cooldownVal = Double.parseDouble(nbtVal.split("\\s+")[0]);
                } catch (Exception ignored) {}
            }

            Vector playerLoc = player.getLocation().toVector();
            Vector hookLoc = hook.getLocation().toVector();
            Vector direction = hookLoc.subtract(playerLoc);
            double distance = direction.length();
            if (distance > 64) distance = 64; 
            
            Vector velocity = direction.clone().normalize().multiply(Math.min(distance * 0.3, 3.5));
            velocity.setY(velocity.getY() * 0.6 + 0.7);
            player.setVelocity(velocity);
            
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
            
            CooldownManager.setCooldown(player.getUniqueId(), "GRAPPLING_HOOK", cooldownVal);
            
            int ticks = (int) (cooldownVal * 20);
            dev.agam.skyblockitems.utils.VisualCooldownUtils.sendVisualCooldown(player, item.getType(), ticks, "GRAPPLING_HOOK");
            
            player.setMetadata("NEGATE_FALL_DAMAGE", new FixedMetadataValue(SkyBlockItems.getInstance(), true));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && player.hasMetadata("NEGATE_FALL_DAMAGE")) {
                        player.removeMetadata("NEGATE_FALL_DAMAGE", SkyBlockItems.getInstance());
                    }
                }
            }.runTaskLater(SkyBlockItems.getInstance(), 100L); 
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
