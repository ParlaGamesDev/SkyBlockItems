package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ButchersBladeAbility extends SkyBlockAbility {

    public ButchersBladeAbility() {
        super("BUTCHERS_BLADE", "להב הקצבים", TriggerType.ON_KILL, 0.0, 0.0, 100.0, 2.0);
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double chance,
            double multiplier) {
        if (!(event instanceof EntityDeathEvent))
            return false;
        EntityDeathEvent e = (EntityDeathEvent) event;

        // Ensure victim is an animal and not an NPC
        if (!(e.getEntity() instanceof org.bukkit.entity.Animals) || dev.agam.skyblockitems.utils.TargetUtils.isNPC(e.getEntity()))
            return false;

        // Double drops
        List<ItemStack> drops = e.getDrops();
        List<ItemStack> bonus = new ArrayList<>();

        for (ItemStack item : drops) {
            if (isMeat(item.getType())) {
                ItemStack copy = item.clone();
                copy.setAmount((int) (item.getAmount() * (multiplier - 1)));
                if (copy.getAmount() > 0)
                    bonus.add(copy);
            }
        }

        for (ItemStack b : bonus) {
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), b);
        }

        return true;
    }

    private boolean isMeat(Material m) {
        String name = m.name();
        return name.contains("BEEF") || name.contains("PORKCHOP") || name.contains("CHICKEN") ||
                name.contains("MUTTON") || name.contains("RABBIT") || name.contains("COD") || name.contains("SALMON");
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
