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
                String cooldownText = ChatColor.RED + "זמן המתנה: " + (int) Math.ceil(remaining) + " שניות";

                // Find where to add cooldown message - after the description lines
                int headerIndex = -1;
                int lastDescriptionIndex = -1;
                for (int i = 0; i < newLore.size(); i++) {
                    String line = ChatColor.stripColor(newLore.get(i));
                    // Get display name from config to ensure match
                    String configName = SkyBlockItems.getInstance().getAbilitiesConfig()
                            .getString("custom-abilities." + abilityId + ".name", ability.getDisplayName());

                    String strippedConfigName = ChatColor
                            .stripColor(dev.agam.skyblockitems.utils.ColorUtils.translate(configName));

                    if (line.contains(strippedConfigName)) {
                        headerIndex = i;
                    }
                    // Description lines start with » or are gray text after header
                    if (headerIndex != -1 && i > headerIndex) {
                        if (line.startsWith("»") || line.startsWith("-") ||
                                (newLore.get(i).startsWith("§7") && !line.isEmpty())) {
                            lastDescriptionIndex = i;
                        } else if (!line.isEmpty() && !newLore.get(i).startsWith(ChatColor.RED + "זמן המתנה: ")) {
                            // Found next section, stop
                            break;
                        }
                    }
                }

                if (headerIndex != -1) {
                    int insertIndex = (lastDescriptionIndex != -1) ? lastDescriptionIndex + 1 : headerIndex + 1;

                    // Check if cooldown line already exists
                    boolean hasCooldownLine = false;
                    for (int i = headerIndex; i < Math.min(insertIndex + 2, newLore.size()); i++) {
                        if (newLore.get(i).startsWith(ChatColor.RED + "זמן המתנה: ")) {
                            // Update existing cooldown line
                            if (!newLore.get(i).equals(cooldownText)) {
                                newLore.set(i, cooldownText);
                                modified = true;
                            }
                            hasCooldownLine = true;
                            break;
                        }
                    }

                    if (!hasCooldownLine && insertIndex <= newLore.size()) {
                        newLore.add(insertIndex, cooldownText);
                        modified = true;
                    }
                }
            } else {
                // If cooldown ended, remove the cooldown line
                for (int i = 0; i < newLore.size(); i++) {
                    if (newLore.get(i).startsWith(ChatColor.RED + "זמן המתנה: ")) {
                        newLore.remove(i);
                        modified = true;
                        break;
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
