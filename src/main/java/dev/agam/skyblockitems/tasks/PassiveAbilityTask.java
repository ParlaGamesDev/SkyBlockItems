package dev.agam.skyblockitems.tasks;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.AbilityManager;
import dev.agam.skyblockitems.abilities.CooldownManager;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Task that runs every second to handle passive abilities.
 */
public class PassiveAbilityTask extends BukkitRunnable {

    private final AbilityManager abilityManager;

    public PassiveAbilityTask() {
        this.abilityManager = SkyBlockItems.getInstance().getAbilityManager();
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processPassiveAbilities(player);
        }
    }

    private void processPassiveAbilities(Player player) {
        // Check main hand
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && !mainHand.getType().isAir()) {
            tryActivatePassiveAbilities(player, mainHand);
        }

        // Check off hand
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && !offHand.getType().isAir()) {
            tryActivatePassiveAbilities(player, offHand);
        }

        // Check armor pieces
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece == null || armorPiece.getType().isAir())
                continue;
            tryActivatePassiveAbilities(player, armorPiece);
        }
    }

    private void tryActivatePassiveAbilities(Player player, ItemStack item) {
        NBTItem nbtItem = NBTItem.get(item);
        if (nbtItem == null || !nbtItem.hasType())
            return;

        for (Map.Entry<String, SkyBlockAbility> entry : abilityManager.getAbilities().entrySet()) {
            String abilityId = entry.getKey();
            SkyBlockAbility ability = entry.getValue();

            String nbtKey = "SKYBLOCK_" + abilityId.toUpperCase();
            if (!nbtItem.hasTag(nbtKey))
                continue;

            String value = nbtItem.getString(nbtKey);
            if (value == null || value.isEmpty())
                continue;

            // Allow PASSIVE, SOLAR_STANCE, and UNDERWATER triggers
            TriggerType trigger = ability.getDefaultTrigger();
            if (trigger != TriggerType.PASSIVE && trigger != TriggerType.SOLAR_STANCE
                    && trigger != TriggerType.UNDERWATER)
                continue;

            // Special handling for Boolean flag abilities
            if (abilityId.equalsIgnoreCase("SOLAR_REPAIR") || abilityId.equalsIgnoreCase("UNDERWATER_MASTER")) {
                ability.activate(player, null, 0, 0, 0, 0);
                continue;
            }

            String[] params = value.trim().split("\\s+");
            double cooldown = 0, mana = 0, damage = 0, range = 0;

            try {
                if (params.length > 0)
                    cooldown = Double.parseDouble(params[0]);
                if (params.length > 1)
                    mana = Double.parseDouble(params[1]);
                if (params.length > 2)
                    damage = Double.parseDouble(params[2]);
                if (params.length > 3)
                    range = Double.parseDouble(params[3]);
            } catch (Exception ignored) {
            }

            if (cooldown > 0 && CooldownManager.isOnCooldown(player.getUniqueId(), abilityId))
                continue;

            // Activate ability
            if (ability.activate(player, null, cooldown, mana, damage, range)) {
                if (cooldown > 0)
                    CooldownManager.setCooldown(player.getUniqueId(), abilityId, cooldown);
            }
        }
    }
}
