package dev.agam.skyblockitems.enchantsystem.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant.EnchantStat;
import dev.agam.skyblockitems.enchantsystem.gui.*;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.Attribute;
import org.bukkit.NamespacedKey;

import java.util.*;

/**
 * Listener for custom enchantment effects and GUI interactions.
 */
public class CustomEnchantListener implements Listener {

    private final SkyBlockItems plugin;
    private final Random random = new Random();

    public CustomEnchantListener(SkyBlockItems plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                applyPeriodicEffects(player);
            }
        }, 20L, 20L);
    }

    private void updateAuraSkillsStats(org.bukkit.entity.Player player) {
        if (!plugin.isAuraSkillsEnabled())
            return;

        var user = plugin.getAuraSkillsHook().getUser(player.getUniqueId());
        if (user == null)
            return;

        // Reset our modifiers first (simplified approach)
        // In a full implementation, we'd use NamespacedIds to clear previous modifiers

        Map<String, Double> totals = new HashMap<>();
        for (ItemStack item : getRelevantItems(player)) {
            if (item == null || item.getType().isAir())
                continue;
            Map<String, Integer> enchants = plugin.getEnchantManager().parseLore(item.getItemMeta().getLore());

            for (var entry : enchants.entrySet()) {
                var conf = plugin.getEnchantManager().getEnchant(entry.getKey());
                if (conf != null && conf.getAuraskillsStat() != null) {
                    var lvlConf = conf.getLevel(entry.getValue());
                    if (lvlConf != null) {
                        totals.put(conf.getAuraskillsStat(),
                                totals.getOrDefault(conf.getAuraskillsStat(), 0.0) + lvlConf.getDoubleValue());
                    }
                }
            }
        }

        // Apply totals
        for (var entry : totals.entrySet()) {
            // This is a placeholder for the actual AuraSkills modifier call
            // user.addModifier(...)
        }
    }

    private void applyPeriodicEffects(Player player) {
        // Stats logic reset to empty slate for user to re-define
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getChatInputManager().hasAwaiting(player)) {
            event.setCancelled(true);
            String message = event.getMessage();

            // Run on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getChatInputManager().handleInput(player, message);
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Defensive stats (Player is the victim)
        if (event.getEntity() instanceof Player victim) {
            applyDefensiveEffects(event, victim);
        }

        // Offensive stats (Player is the damager)
        if (!(event.getDamager() instanceof Player player))
            return;
        if (!(event.getEntity() instanceof LivingEntity target))
            return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon == null || !weapon.hasItemMeta() || !weapon.getItemMeta().hasLore())
            return;

        List<String> lore = weapon.getItemMeta().getLore();
        Map<CustomEnchant, Integer> appliedEnchants = parseCustomEnchants(lore);

        for (Map.Entry<CustomEnchant, Integer> entry : appliedEnchants.entrySet()) {
            CustomEnchant enchant = entry.getKey();
            int level = entry.getValue();

            applyDamageEffects(event, player, target, enchant, level);
        }
    }

    private void applyDefensiveEffects(EntityDamageByEntityEvent event, Player victim) {
        // Stats logic reset to empty slate for user to re-define
    }

    private Map<CustomEnchant, Integer> parseCustomEnchants(List<String> lore) {
        Map<CustomEnchant, Integer> result = new HashMap<>();

        for (String line : lore) {
            String cleanLine = ChatColor.stripColor(line);

            // Parse comma-separated enchants
            String[] entries = cleanLine.split(",");
            for (String entry : entries) {
                String trimmed = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', entry.trim()));
                if (trimmed.isEmpty())
                    continue;

                int spaceIndex = trimmed.lastIndexOf(' ');
                String namePart = trimmed;
                int level = 1;

                if (spaceIndex != -1) {
                    namePart = trimmed.substring(0, spaceIndex);
                    String levelPart = trimmed.substring(spaceIndex + 1);
                    int parsedLevel = fromRoman(levelPart);
                    if (parsedLevel > 0) {
                        level = parsedLevel;
                    } else {
                        spaceIndex = -1;
                        namePart = trimmed;
                        level = 1;
                    }
                }

                for (CustomEnchant enchant : plugin.getCustomEnchantManager().getAllEnchants()) {
                    String enchantName = ChatColor.stripColor(
                            ChatColor.translateAlternateColorCodes('&', enchant.getDisplayName()));

                    if (enchantName.equalsIgnoreCase(trimmed) && enchant.getMaxLevel() == 1) {
                        result.put(enchant, 1);
                        break;
                    } else if (spaceIndex != -1 && enchantName.equalsIgnoreCase(namePart) && level > 0) {
                        result.put(enchant, level);
                        break;
                    } else if (spaceIndex == -1 && enchantName.equalsIgnoreCase(namePart)
                            && enchant.getMaxLevel() == 1) {
                        result.put(enchant, 1);
                        break;
                    }
                }
            }
        }

        return result;
    }

    private void applyDamageEffects(EntityDamageByEntityEvent event, Player player,
            LivingEntity target, CustomEnchant enchant, int level) {
        // Stats logic reset to empty slate for user to re-define
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // Stats logic reset to empty slate for user to re-define
    }

    @EventHandler
    public void onExp(org.bukkit.event.player.PlayerExpChangeEvent event) {
        // Stats logic reset to empty slate for user to re-define
    }

    // Simplified periodic logic

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker))
            return;
        if (!(event.getEntity() instanceof LivingEntity victim))
            return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || !weapon.hasItemMeta() || !weapon.getItemMeta().hasLore())
            return;
        Map<String, Integer> enchants = plugin.getEnchantManager().parseLore(weapon.getItemMeta().getLore());

        // 1. Life Steal
        if (enchants.containsKey("life_steal")) {
            int level = enchants.get("life_steal");
            double healAmount = event.getFinalDamage() * (level * 0.05); // 5% per level
            double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + healAmount));
        }

        // 2. Extra Damage (Smite/Bane logic if not vanilla)
        if (victim instanceof org.bukkit.entity.Zombie || victim instanceof org.bukkit.entity.Skeleton) {
            if (enchants.containsKey("smite")
                    && !weapon.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.SMITE)) {
                event.setDamage(event.getDamage() + (enchants.get("smite") * 2.5));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTakeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim))
            return;
        if (!(event.getDamager() instanceof LivingEntity attacker))
            return;

        // Damage Reflection
        for (ItemStack armor : victim.getInventory().getArmorContents()) {
            if (armor == null || armor.getType().isAir() || !armor.hasItemMeta() || !armor.getItemMeta().hasLore())
                continue;
            Map<String, Integer> enchants = plugin.getEnchantManager().parseLore(armor.getItemMeta().getLore());

            if (enchants.containsKey("reflection")) {
                int level = enchants.get("reflection");
                double reflectDamage = event.getDamage() * (level * 0.1); // 10% reflection
                attacker.damage(reflectDamage, victim);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool == null || !tool.hasItemMeta() || !tool.getItemMeta().hasLore())
            return;

        Map<String, Integer> enchants = plugin.getEnchantManager().parseLore(tool.getItemMeta().getLore());

        if (enchants.containsKey("telepath") || enchants.containsKey("telekinesis")) {
            event.setDropItems(false);
            for (ItemStack drop : event.getBlock().getDrops(tool)) {
                event.getPlayer().getInventory().addItem(drop).values()
                        .forEach(i -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(),
                                i));
            }
        }
    }

    private List<ItemStack> getRelevantItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        items.add(player.getInventory().getItemInMainHand());
        items.add(player.getInventory().getItemInOffHand());
        items.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        items.removeIf(i -> i == null || !i.hasItemMeta() || !i.getItemMeta().hasLore());
        return items;
    }

    private int fromRoman(String roman) {
        return switch (roman.toUpperCase()) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            case "VI" -> 6;
            case "VII" -> 7;
            case "VIII" -> 8;
            case "IX" -> 9;
            case "X" -> 10;
            default -> 0;
        };
    }
}
