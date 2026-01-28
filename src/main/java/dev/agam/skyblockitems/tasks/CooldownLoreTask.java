package dev.agam.skyblockitems.tasks;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.CooldownManager;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CooldownLoreTask extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerInventory(player);
        }
    }

    private void updatePlayerInventory(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir())
            return;

        NBTItem nbtItem = NBTItem.get(item);
        if (nbtItem == null || !nbtItem.hasType())
            return;

        boolean modified = false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore())
            return;

        List<String> lore = meta.getLore();
        List<String> newLore = new ArrayList<>(lore);

        for (Map.Entry<String, SkyBlockAbility> entry : SkyBlockItems.getInstance().getAbilityManager().getAbilities()
                .entrySet()) {
            String abilityId = entry.getKey();
            SkyBlockAbility ability = entry.getValue();

            String nbtKey = "SKYBLOCK_" + abilityId.toUpperCase();
            if (!nbtItem.hasTag(nbtKey))
                continue;

            if (CooldownManager.isOnCooldown(player.getUniqueId(), abilityId)) {
                double remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), abilityId);
                String cooldownText = ChatColor.RED + ability.getDisplayName() + " בטעינה: "
                        + String.format("%.1f", remaining) + " ש'";

                // Find where to replace in lore
                // We look for a line that starts with the ability header format (simplified
                // check)
                int headerIndex = -1;
                for (int i = 0; i < newLore.size(); i++) {
                    String line = ChatColor.stripColor(newLore.get(i));
                    if (line.contains(ability.getDisplayName())) {
                        headerIndex = i;
                        break;
                    }
                }

                if (headerIndex != -1) {
                    // Check if already showing cooldown to prevent redundant updates
                    if (!newLore.get(headerIndex).contains("בטעינה")) {
                        // Store original lore line in metadata or NBT if needed for restoration?
                        // Actually, MMOItems will rebuild the item anyway, but we want it to look
                        // "live"
                        newLore.set(headerIndex, cooldownText);
                        // Hide subsequent description lines for this ability?
                        // For now just replacing the header for simplicity and clarity
                        modified = true;
                    } else if (!newLore.get(headerIndex).equals(cooldownText)) {
                        newLore.set(headerIndex, cooldownText);
                        modified = true;
                    }
                }
            } else {
                // If it was on cooldown but now isn't, we need to restore it.
                // Rebuilding via MMOItems is the safest way to get the correct lore back.
                // However, doing it every tick is heavy. We only do it once.
                for (int i = 0; i < newLore.size(); i++) {
                    if (newLore.get(i).contains("בטעינה") && newLore.get(i).contains(ability.getDisplayName())) {
                        // Needs restoration
                        restoreItem(player, item);
                        return; // Exit loop, item will be updated next tick if needed
                    }
                }
            }
        }

        if (modified) {
            meta.setLore(newLore);
            item.setItemMeta(meta);
        }
    }

    private void restoreItem(Player player, ItemStack item) {
        // Trigger a rebuild of the item lore via MMOItems
        NBTItem nbt = NBTItem.get(item);
        String type = nbt.getType();
        String id = nbt.getString("MMOITEMS_ITEM_ID");

        if (type != null && id != null) {
            ItemStack newItem = net.Indyuce.mmoitems.MMOItems.plugin.getItem(net.Indyuce.mmoitems.api.Type.get(type),
                    id);
            if (newItem != null) {
                // Keep some properties like enchants or custom data if necessary?
                // Standard SkyBlock items usually just follow the template.
                player.getInventory().setItemInMainHand(newItem);
            }
        }
    }
}
