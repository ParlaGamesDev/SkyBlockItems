package dev.agam.skyblockitems.abilities.combat;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SolarRepairAbility extends SkyBlockAbility implements Listener {

    public SolarRepairAbility() {
        super("SOLAR_REPAIR", "טעינה סולארית", TriggerType.SOLAR_STANCE, 0.0, 0.0, 1.0, 50.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        // Called every second by PassiveAbilityTask

        // 1. Environmental Checks
        int light = player.getEyeLocation().getBlock().getLightFromSky();
        boolean hasStorm = player.getWorld().hasStorm();
        long time = player.getWorld().getTime();
        boolean isSunTime = (time >= 0 && time <= 12500);

        if (light < 12 || !isSunTime || hasStorm)
            return false;

        // 2. Load settings from abilities config
        org.bukkit.configuration.file.FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems
                .getInstance()
                .getAbilitiesConfig();
        double amount = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.charge-amount", 1.0);
        int intervalSeconds = abilitiesConfig.getInt("custom-abilities.SOLAR_REPAIR.charge-interval", 30);
        double maxCharge = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.max-charge", 120.0);

        boolean updated = false;
        long now = System.currentTimeMillis();

        // Process Main Hand
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack updatedMain = processSingleItem(player, main, amount, maxCharge, intervalSeconds, now);
        if (updatedMain != null) {
            player.getInventory().setItemInMainHand(updatedMain);
            updated = true;
        }

        // Process Off Hand
        ItemStack off = player.getInventory().getItemInOffHand();
        ItemStack updatedOff = processSingleItem(player, off, amount, maxCharge, intervalSeconds, now);
        if (updatedOff != null) {
            player.getInventory().setItemInOffHand(updatedOff);
            updated = true;
        }

        // Process Armor
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean armorChanged = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack updatedPiece = processSingleItem(player, armor[i], amount, maxCharge, intervalSeconds, now);
            if (updatedPiece != null) {
                armor[i] = updatedPiece;
                armorChanged = true;
                updated = true;
            }
        }
        if (armorChanged)
            player.getInventory().setArmorContents(armor);

        return updated;
    }

    private ItemStack processSingleItem(Player player, ItemStack item, double rate, double max, int intervalSec,
            long now) {
        if (item == null || item.getType().isAir())
            return null;

        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasTag("SKYBLOCK_SOLAR_REPAIR"))
            return null;

        // Check Interval (Use String for absolute precision with Millis)
        String lastUpdateStr = nbt.hasTag("SOLAR_LAST_UPDATE") ? nbt.getString("SOLAR_LAST_UPDATE") : "0";
        long lastUpdate = Long.parseLong(lastUpdateStr);

        if (now - lastUpdate < (long) intervalSec * 1000)
            return null;

        double current = nbt.hasTag("SOLAR_CHARGE_VAL") ? nbt.getDouble("SOLAR_CHARGE_VAL") : 0.0;
        if (current >= max)
            return null;

        double next = Math.min(max, current + rate);

        // Update NBT with both value and new timestamp
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("SOLAR_CHARGE_VAL", next));
        nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("SOLAR_LAST_UPDATE", String.valueOf(now)));
        ItemStack updated = nbt.toItem();

        updateLore(updated, next, max);

        // Effects
        player.getWorld().spawnParticle(org.bukkit.Particle.DUST, player.getLocation().add(0, 1.5, 0), 8,
                new org.bukkit.Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f));
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);

        return updated;
    }

    private void updateLore(ItemStack item, double charge, double max) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        String chargeLine = dev.agam.skyblockitems.utils.ColorUtils.translate(
                "&7נזק שנצבר: &6" + String.format("%.1f", charge) + " נזק מתוך " + (int) max);

        int index = -1;
        for (int i = 0; i < lore.size(); i++) {
            String stripped = org.bukkit.ChatColor.stripColor(lore.get(i));
            if (stripped.contains("נזק שנצבר:")) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            lore.set(index, chargeLine);
            meta.setLore(lore);
            item.setItemMeta(meta);
        } else {
            // If it can't find the line, it might be due to a desync in AbilityStat
            // injection.
            // Let's add it regardless if we have space, to ensure it shows up.
            // lore.add("");
            // lore.add(chargeLine);
            // meta.setLore(lore);
            // item.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player))
            return;
        Player player = (Player) event.getDamager();

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir())
            return;

        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasTag("SOLAR_CHARGE_VAL"))
            return;

        double charge = nbt.getDouble("SOLAR_CHARGE_VAL");
        if (charge <= 0.1)
            return; // Tiny threshold

        // 1. Apply Damage
        event.setDamage(event.getDamage() + charge);

        // 2. Reset Item
        org.bukkit.configuration.file.FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems
                .getInstance()
                .getAbilitiesConfig();
        double maxCharge = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.max-charge", 50.0);

        // Update tag to 0
        ItemStack resetItem = nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag("SOLAR_CHARGE_VAL", 0.0)).toItem();
        updateLore(resetItem, 0.0, maxCharge);
        player.getInventory().setItemInMainHand(resetItem);

        // 3. Effects
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
        player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, event.getEntity().getLocation().add(0, 1, 0), 15,
                0.3, 0.3, 0.3, 0.1);
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
