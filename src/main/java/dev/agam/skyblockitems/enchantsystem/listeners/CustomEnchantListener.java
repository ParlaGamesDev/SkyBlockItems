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
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.attribute.Attribute;

/**
 * Listener for custom enchantment effects and GUI interactions.
 */
public class CustomEnchantListener implements Listener {

    private final SkyBlockItems plugin;
    private final Random random = new Random();

    // Internal trackers
    private final Map<UUID, Integer> miningCombo = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMineTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastGreedTime = new ConcurrentHashMap<>();
    private final Map<UUID, Double> soulCharge = new ConcurrentHashMap<>();

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

    private void updateAuraSkillsStats(Player player) {
        if (!plugin.isAuraSkillsEnabled())
            return;

        Map<String, Double> totals = new HashMap<>();
        for (ItemStack item : getRelevantItems(player)) {
            if (item == null || item.getType().isAir())
                continue;

            // Collect enchants from both enchants.yml and custom-enchants.yml
            Map<String, Integer> enchants = plugin.getEnchantManager().parseLore(item.getItemMeta().getLore());
            enchants.putAll(parseEnchantIds(item.getItemMeta().getLore()));

            for (var entry : enchants.entrySet()) {
                var conf = plugin.getEnchantManager().getEnchant(entry.getKey());
                String statName = null;
                if (conf != null) {
                    statName = conf.getAuraskillsStat();
                } else {
                    // Check custom enchants
                    var customConf = plugin.getCustomEnchantManager().getEnchant(entry.getKey());
                    if (customConf != null) {
                        // Custom enchants might use a different stat mapping or just hardcode for now
                        if (customConf.getId().equalsIgnoreCase("STRENGTH_BOOST"))
                            statName = "strength";
                        // Add more mappings if needed
                    }
                }

                if (statName != null) {
                    double perLevel = 1.0; // Default or from config if available
                    totals.put(statName, totals.getOrDefault(statName, 0.0) + (entry.getValue() * perLevel));
                }
            }
        }

        // Apply totals via hook
        var hook = plugin.getAuraSkillsHook();
        // Clear old modifiers (AuraSkills modifiers with same ID overwrite, but we
        // should handle removal if 0)
        String[] possibleStats = { "strength", "wisdom", "luck", "health", "regeneration", "toughness", "crit_chance",
                "crit_damage" };
        for (String stat : possibleStats) {
            double total = totals.getOrDefault(stat, 0.0);
            if (total > 0) {
                hook.addStatModifier(player, stat, "SBI_ENCHANT_" + stat.toUpperCase(), total);
            } else {
                hook.removeStatModifier(player, "SBI_ENCHANT_" + stat.toUpperCase());
            }
        }
    }

    private Map<String, Integer> parseEnchantIds(List<String> lore) {
        Map<String, Integer> result = new HashMap<>();
        if (lore == null)
            return result;
        Map<CustomEnchant, Integer> custom = parseCustomEnchants(lore);
        for (var entry : custom.entrySet()) {
            result.put(entry.getKey().getId().toLowerCase(), entry.getValue());
        }
        return result;
    }

    private void applyPeriodicEffects(Player player) {
        updateAuraSkillsStats(player);
        // Periodic effects for enchants can be added here
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
        ItemStack[] armor = victim.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null || !piece.hasItemMeta() || !piece.getItemMeta().hasLore())
                continue;
            Map<CustomEnchant, Integer> enchants = parseCustomEnchants(piece.getItemMeta().getLore());

            for (CustomEnchant e : enchants.keySet()) {
                if (!e.isEnabled())
                    continue;

        // Other defensive effects
    }

    private Map<CustomEnchant, Integer> parseCustomEnchants(List<String> lore) {
        Map<CustomEnchant, Integer> result = new HashMap<>();

        for (String line : lore) {
            String cleanLine = dev.agam.skyblockitems.enchantsystem.utils.ColorUtils.stripColor(line);

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
        if (!enchant.isEnabled())
            return;

        // 1. BERSERK
        if (enchant.getId().equalsIgnoreCase("BERSERK")) {
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            event.setDamage(event.getDamage() * 1.5);
        }

        // 2. MONSTER_SOUL (Release)
        if (enchant.getId().equalsIgnoreCase("MONSTER_SOUL")) {
            double charge = soulCharge.getOrDefault(player.getUniqueId(), 0.0);
            if (charge > 0) {
                // Check if it's a critical hit (player falling, etc)
                boolean isCrit = player.getFallDistance() > 0.0F && !player.isInsideVehicle()
                        && !player.hasPotionEffect(PotionEffectType.BLINDNESS);

                if (isCrit) {
                    event.setDamage(event.getDamage() + charge);
                    soulCharge.put(player.getUniqueId(), 0.0);
                    player.sendMessage(ChatColor.DARK_PURPLE + "Soul strike unleashed!");
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            org.bukkit.block.Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.FARMLAND) {
                Player player = event.getPlayer();
                ItemStack boots = player.getInventory().getBoots();
                if (boots != null && boots.hasItemMeta() && boots.getItemMeta().hasLore()) {
                    Map<CustomEnchant, Integer> enchants = parseCustomEnchants(boots.getItemMeta().getLore());
                    if (enchants.keySet().stream()
                            .anyMatch(e -> e.getId().equalsIgnoreCase("TRAMPLE_GUARD") && e.isEnabled())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityKill(org.bukkit.event.entity.EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null)
            return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon == null || !weapon.hasItemMeta() || !weapon.getItemMeta().hasLore())
            return;

        Map<CustomEnchant, Integer> enchants = parseCustomEnchants(weapon.getItemMeta().getLore());
        for (CustomEnchant e : enchants.keySet()) {
            if (!e.isEnabled())
                continue;

            // 1. MONSTER_SOUL (Charge)
            if (e.getId().equalsIgnoreCase("MONSTER_SOUL")) {
                // User said: "פי 10 מנזק המפלצת שנהרגה"
                // Assuming we use the victim's max health or last damage as "damage of monster"
                double bonus = (victim.getAttribute(Attribute.ATTACK_DAMAGE) != null
                        ? victim.getAttribute(Attribute.ATTACK_DAMAGE).getValue()
                        : 10.0) * 10.0;
                soulCharge.put(killer.getUniqueId(), bonus);
                killer.sendMessage(ChatColor.GRAY + "Soul harvested! Next strike will be powered.");
            }
        }
    }

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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || !tool.hasItemMeta() || !tool.getItemMeta().hasLore())
            return;

        Map<CustomEnchant, Integer> enchants = parseCustomEnchants(tool.getItemMeta().getLore());
        if (enchants.isEmpty())
            return;

        org.bukkit.block.Block block = event.getBlock();
        Material type = block.getType();

        // 1. DELICATE (Moנע שבירת יבולים צעירים)
        if (isDelicateEnabled(enchants)) {
            if (isYoungCrop(block)) {
                event.setCancelled(true);
                return;
            }
        }

        // 2. REPLENISH (שתילה מחדש אוטומטית)
        if (isReplenishEnabled(enchants) && isFullyGrownCrop(block)) {
            Material seedType = getSeedForCrop(type);
            if (seedType != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    block.setType(type);
                    if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                        ageable.setAge(0);
                        block.setBlockData(ageable);
                    }
                }, 1L);
            }
        }

        // 3. FARMING_FORTUNE & HARVESTING (דאבל/טריפל דרופ)
        int fortuneLevel = getFortuneLevel(enchants, type);
        if (fortuneLevel > 0) {
            // Only work on fully grown crops for both
            if (isCrop(type) && !isFullyGrownCrop(block))
                return;

            // HARVESTING specific: Only natural logs
            boolean isHarvesting = enchants.keySet().stream().anyMatch(e -> e.getId().equalsIgnoreCase("HARVESTING"));
            if (isHarvesting && isLog(type) && block.hasMetadata("PLACED_BY_PLAYER"))
                return;

            double chance = fortuneLevel * 0.25; // 25% chance for extra drop per level
            if (random.nextDouble() < chance) {
                Collection<ItemStack> drops = block.getDrops(tool);
                for (ItemStack drop : drops) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    if (random.nextDouble() < (chance - 1.0)) { // Triple drop chance if level > 1
                        block.getWorld().dropItemNaturally(block.getLocation(), drop);
                    }
                }
            }
        }

        // 4. TURBO_MINER (מהירות כרייה)
        if (isTurboMinerEnabled(enchants)) {
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();
            long last = lastMineTime.getOrDefault(uuid, 0L);

            if (now - last > 2000) { // 2s timeout (User requested)
                miningCombo.put(uuid, 1);
            } else {
                int combo = miningCombo.getOrDefault(uuid, 0) + 1;
                miningCombo.put(uuid, combo);

                // User said: שיחכה 4 בלוקים שהוא חוצב ברצף ואז הוא מפעיל את הטורבו
                if (combo >= 4) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, 2)); // 3s Haste III
                }
            }
            lastMineTime.put(uuid, now);
        }

        // Existing Telekinesis logic
        if (enchants.keySet().stream().anyMatch(e -> e.getId().equalsIgnoreCase("TELEKINESIS"))) {
            event.setDropItems(false);
            for (ItemStack drop : block.getDrops(tool)) {
                player.getInventory().addItem(drop).values()
                        .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            }
        }
    }

    private boolean isDelicateEnabled(Map<CustomEnchant, Integer> enchants) {
        return enchants.entrySet().stream()
                .anyMatch(e -> e.getKey().getId().equalsIgnoreCase("DELICATE") && e.getKey().isEnabled());
    }

    private boolean isReplenishEnabled(Map<CustomEnchant, Integer> enchants) {
        return enchants.entrySet().stream()
                .anyMatch(e -> e.getKey().getId().equalsIgnoreCase("REPLENISH") && e.getKey().isEnabled());
    }

    private boolean isTurboMinerEnabled(Map<CustomEnchant, Integer> enchants) {
        return enchants.entrySet().stream()
                .anyMatch(e -> e.getKey().getId().equalsIgnoreCase("TURBO_MINER") && e.getKey().isEnabled());
    }

    private int getFortuneLevel(Map<CustomEnchant, Integer> enchants, Material block) {
        for (Map.Entry<CustomEnchant, Integer> entry : enchants.entrySet()) {
            CustomEnchant e = entry.getKey();
            if (!e.isEnabled())
                continue;

            if (e.getId().equalsIgnoreCase("FARMING_FORTUNE") && isCrop(block))
                return entry.getValue();
            if (e.getId().equalsIgnoreCase("HARVESTING") && (isLog(block) || isCrop(block)))
                return entry.getValue();
        }
        return 0;
    }

    private boolean isCrop(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOTS
                || m == Material.NETHER_WART || m == Material.COCOA_BEANS;
    }

    private boolean isLog(Material m) {
        return m.name().contains("_LOG") || m.name().contains("_WOOD");
    }

    private boolean isFullyGrownCrop(org.bukkit.block.Block b) {
        if (b.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private boolean isYoungCrop(org.bukkit.block.Block b) {
        if (b.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            return ageable.getAge() < ageable.getMaximumAge();
        }
        return false;
    }

    private Material getSeedForCrop(Material crop) {
        return switch (crop) {
            case WHEAT -> Material.WHEAT_SEEDS;
            case CARROTS -> Material.CARROT;
            case POTATOES -> Material.POTATO;
            case BEETROOTS -> Material.BEETROOT_SEEDS;
            case NETHER_WART -> Material.NETHER_WART;
            default -> null;
        };
    }

    public Map<CustomEnchant, Integer> getEffectiveEnchants(Player player) {
        Map<CustomEnchant, Integer> totals = new HashMap<>();
        for (ItemStack item : getRelevantItems(player)) {
            Map<CustomEnchant, Integer> enchants = parseCustomEnchants(item.getItemMeta().getLore());
            for (Map.Entry<CustomEnchant, Integer> entry : enchants.entrySet()) {
                if (!entry.getKey().isEnabled())
                    continue;
                totals.put(entry.getKey(), Math.max(totals.getOrDefault(entry.getKey(), 0), entry.getValue()));
            }
        }
        return totals;
    }

    public void handleManaSpend(Player player, double manaSpent) {
        // Guardian Mana removed
    }

    private List<ItemStack> getRelevantItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        items.add(player.getInventory().getItemInMainHand());
        items.add(player.getInventory().getItemInOffHand());
        items.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        items.removeIf(i -> i == null || !i.hasItemMeta() || !i.getItemMeta().hasLore());
        return items;
    }

    public int getEffectiveEnchantLevel(Player player, String id) {
        return getEffectiveEnchants(player).entrySet().stream()
                .filter(e -> e.getKey().getId().equalsIgnoreCase(id))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0);
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
