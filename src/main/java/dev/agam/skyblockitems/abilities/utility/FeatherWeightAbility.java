package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FeatherWeightAbility extends SkyBlockAbility implements Listener {

    public FeatherWeightAbility() {
        super("FEATHER_WEIGHT", "משקל נוצה", TriggerType.PASSIVE, 0.0, 0.0, 50.0, 0.0);
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double reductionPercent,
            double range) {
        // Handled by Event
        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;
        if (!(event.getEntity() instanceof Player))
            return;

        Player player = (Player) event.getEntity();
        double totalReduction = 0;

        // Check armor pieces for the tag
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType().isAir())
                continue;

            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(armor);
            String tag = nbt.getString("SKYBLOCK_FEATHER_WEIGHT");
            if (tag != null && !tag.isEmpty()) {
                try {
                    String[] params = tag.trim().split("\\s+");
                    // Format: COOLDOWN MANA REDUCTION_PERCENT
                    // params[0] = cooldown, params[1] = mana, params[2] = reduction percent
                    if (params.length >= 3) {
                        double reduction = Double.parseDouble(params[2]);
                        totalReduction += reduction;
                    } else if (params.length == 1) {
                        // Fallback: if only one param, treat it as reduction percent
                        totalReduction += Double.parseDouble(params[0]);
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid number format, skip this item
                }
            }
        }

        if (totalReduction > 0) {
            double multiplier = Math.max(0, 1 - (totalReduction / 100.0));
            event.setDamage(event.getDamage() * multiplier);
            if (multiplier == 0)
                event.setCancelled(true);

            player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, player.getLocation(), 5, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
