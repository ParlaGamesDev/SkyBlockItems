package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExplosiveArrowAbility extends SkyBlockAbility implements Listener {

    private final Map<Arrow, TNTPrimed> activeTNT = new HashMap<>();

    public ExplosiveArrowAbility() {
        super("EXPLOSIVE_ARROW", "חץ נפץ", TriggerType.ON_ARROW_HIT, 10.0, 30.0, 8.0, 4.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        if (!(event instanceof ProjectileHitEvent))
            return false;
        ProjectileHitEvent e = (ProjectileHitEvent) event;

        if (!(e.getEntity() instanceof Arrow))
            return false;
        Arrow arrow = (Arrow) e.getEntity();

        TNTPrimed tnt = activeTNT.remove(arrow);
        if (tnt != null)
            tnt.remove();

        Location explodeLoc = e.getHitEntity() != null ? e.getHitEntity().getLocation()
                : (e.getHitBlock() != null ? e.getHitBlock().getLocation() : arrow.getLocation());

        // Effects
        explodeLoc.getWorld().createExplosion(explodeLoc, 0, false, false);
        explodeLoc.getWorld().playSound(explodeLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        explodeLoc.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_EMITTER, explodeLoc, 1);

        // Damage logic
        for (Entity nearby : explodeLoc.getWorld().getNearbyEntities(explodeLoc, range, range, range)) {
            if (nearby instanceof LivingEntity && nearby != player) {
                ((LivingEntity) nearby).damage(damage, player);
            }
        }

        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();
        if (!(event.getProjectile() instanceof Arrow))
            return;
        Arrow arrow = (Arrow) event.getProjectile();

        NBTItem nbtItem = NBTItem.get(event.getBow());
        if (nbtItem == null || !nbtItem.hasTag("SKYBLOCK_EXPLOSIVE_ARROW"))
            return;

        if (dev.agam.skyblockitems.abilities.CooldownManager.isOnCooldown(player.getUniqueId(), "EXPLOSIVE_ARROW"))
            return;

        // Spawn TNT and attach to arrow
        TNTPrimed tnt = arrow.getWorld().spawn(arrow.getLocation(), TNTPrimed.class, t -> {
            t.setFuseTicks(100);
            t.setYield(0);
            t.setIsIncendiary(false);
        });

        arrow.addPassenger(tnt);
        activeTNT.put(arrow, tnt);

        // Auto-cleanup task (just in case)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround() || !arrow.isValid()) {
                    TNTPrimed removed = activeTNT.remove(arrow);
                    if (removed != null)
                        removed.remove();
                    cancel();
                }
            }
        }.runTaskTimer(SkyBlockItems.getInstance(), 40L, 20L);
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>(); // Handled by AbilityStat.java
    }
}
