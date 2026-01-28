package dev.agam.skyblockitems.abilities.utility;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MetabolismAbility extends SkyBlockAbility implements Listener {

    public MetabolismAbility() {
        super("SLOW_METABOLISM", "מטבוליזם איטי", TriggerType.PASSIVE, 0.0, 0.0, 50.0, 0.0);
        org.bukkit.Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double reductionPercent,
            double range) {
        return true;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHungerChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();

        // Only trigger if food level is decreasing
        if (event.getFoodLevel() >= player.getFoodLevel())
            return;

        double totalReduction = 0;

        // Scan armor and artifacts
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack armor : player.getInventory().getArmorContents())
            if (armor != null)
                items.add(armor);
        for (ItemStack hotbar : player.getInventory().getContents()) {
            if (hotbar != null && !hotbar.getType().isAir()) {
                // Check if it's an accessory or in hotbar (simplified)
                items.add(hotbar);
            }
        }

        for (ItemStack item : items) {
            io.lumine.mythic.lib.api.item.NBTItem nbt = io.lumine.mythic.lib.api.item.NBTItem.get(item);
            String tag = nbt.getString("SKYBLOCK_SLOW_METABOLISM");
            if (tag != null && !tag.isEmpty()) {
                try {
                    String[] params = tag.trim().split("\\s+");
                    if (params.length > 2) {
                        totalReduction += Double.parseDouble(params[2]);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (totalReduction > 0) {
            double chanceToNegate = totalReduction / 100.0;
            if (new java.util.Random().nextDouble() < chanceToNegate) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
