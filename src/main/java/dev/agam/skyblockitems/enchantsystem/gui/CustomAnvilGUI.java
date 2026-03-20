package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hypixel SkyBlock-style Anvil GUI based on user's diagram.
 * Layout: 45 slots (5 rows)
 * - Item 2 (Sacrifice): Slot 4 (top center)
 * - Result: Slot 24 (right side)
 * - Info Button: Slot 18 (left side)
 * - Item 1 (Target) + Combine: Slot 22 (center)
 */
public class CustomAnvilGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;

    // Slot positions based on user diagram (0-indexed)
    private static final int ITEM_1_SLOT = 10; // Row 2, Col 2 - Target (Top)
    private static final int RESULT_SLOT = 24; // Row 3, Col 7 - Result
    private static final int ITEM_2_SLOT = 28; // Row 4, Col 2 - Sacrifice (Bottom)
    private static final int COMBINE_SLOT = 21; // Row 3, Col 4 - Anvil

    public CustomAnvilGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, plugin.getConfigManager().getMessage("anvil.gui-title"));
        setupGUI();
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        // Clear functional slots
        inventory.setItem(ITEM_1_SLOT, null);
        inventory.setItem(ITEM_2_SLOT, null);

        // Result placeholder (gray)
        inventory.setItem(RESULT_SLOT, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.anvil-result-placeholder"),
                Material.GRAY_DYE));

        // Combine button
        updateCombineButton(false, 0, "empty");

        // Initial indicators
        updateIndicators(false);

        // Fill remaining empty slots with configured glass pane
        Material fillerMat = Material
                .getMaterial(plugin.getConfig().getString("gui.anvil.filler-material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            // Don't fill the input slots, even if they are null
            if (i == ITEM_1_SLOT || i == ITEM_2_SLOT)
                continue;

            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private void updateIndicators(boolean valid) {
        ItemStack indicator = ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection(
                        valid ? "gui.items.anvil-indicator-valid" : "gui.items.anvil-indicator-invalid"),
                valid ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);

        // Background highlighting around functional areas (around slot 24)
        int[] resultArea = { 14, 15, 16, 23, 25, 32, 33, 34 };
        for (int slot : resultArea) {
            inventory.setItem(slot, indicator);
        }
    }

    private void updateCombineButton(boolean valid, int cost, String reasonKey) {
        String key = valid ? "gui.items.anvil-combine-valid" : "gui.items.anvil-combine-invalid";
        ItemStack anvil = ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection(key), Material.ANVIL);

        ItemMeta meta = anvil.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

            // If invalid, add reason to lore
            if (!valid && reasonKey != null) {
                String reason = plugin.getConfigManager().getMessage("anvil.reason." + reasonKey);
                lore.add("");
                lore.add(ColorUtils.colorize(reason));
            }

            List<String> finalLore = lore.stream()
                    .map(line -> line.replace("{cost}", String.valueOf(cost)))
                    .map(ColorUtils::colorize)
                    .collect(Collectors.toList());

            meta.setLore(finalLore);
            anvil.setItemMeta(meta);
        }

        inventory.setItem(COMBINE_SLOT, anvil);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;

        int slot = event.getRawSlot();
        boolean isTopInv = slot < 45;

        if (!isTopInv) {
            ItemStack clicked = event.getCurrentItem();
            if (isEmpty(clicked))
                return;

            // Blacklist check for Anvil
            if (plugin.getConfigManager().isBlacklisted(clicked)) {
                player.sendMessage(plugin.getConfigManager().getMessage("errors.blacklisted-item"));
                event.setCancelled(true);
                return;
            }

            // MMOItems Disable Anvil Check
            if (io.lumine.mythic.lib.api.item.NBTItem.get(clicked).hasTag("MMOITEMS_DISABLE_ANVIL")) {
                player.sendMessage(plugin.getConfigManager().getMessage("errors.item-disabled-anvil"));
                event.setCancelled(true);
                return;
            }

            // Auto-slot logic: Find first empty slot
            if (isEmpty(inventory.getItem(ITEM_1_SLOT))) {
                inventory.setItem(ITEM_1_SLOT, clicked.clone());
                event.setCurrentItem(null);
            } else if (isEmpty(inventory.getItem(ITEM_2_SLOT))) {
                inventory.setItem(ITEM_2_SLOT, clicked.clone());
                event.setCurrentItem(null);
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("errors.cannot-place-here"));
                event.setCancelled(true);
                return;
            }

            Bukkit.getScheduler().runTaskLater(plugin, this::updateResult, 1L);
            return;
        }

        // Handle item removal from input slots
        if (slot == ITEM_1_SLOT || slot == ITEM_2_SLOT) {
            ItemStack clicked = inventory.getItem(slot);
            if (!isEmpty(clicked)) {
                // Return to player
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(clicked);
                if (!leftover.isEmpty()) {
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                inventory.setItem(slot, null);
                Bukkit.getScheduler().runTaskLater(plugin, this::updateResult, 1L);
            }
            event.setCancelled(true);
            return;
        }

        if (slot == COMBINE_SLOT) {
            event.setCancelled(true);
            handleCombine();
            return;
        }

        if (slot == RESULT_SLOT) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this)
            return;

        for (int slot : event.getRawSlots()) {
            if (slot < 45 && slot != ITEM_1_SLOT && slot != ITEM_2_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::updateResult, 1L);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        returnItem(ITEM_1_SLOT);
        returnItem(ITEM_2_SLOT);
    }

    private void returnItem(int slot) {
        ItemStack item = inventory.getItem(slot);
        if (item != null && !item.getType().isAir()) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }
    }

    private void updateResult() {
        ItemStack item1 = inventory.getItem(ITEM_1_SLOT);
        ItemStack item2 = inventory.getItem(ITEM_2_SLOT);

        if (isEmpty(item1) && isEmpty(item2)) {
            resetResult();
            return;
        }

        if (isEmpty(item1) || isEmpty(item2)) {
            resetResult();
            return;
        }

        // Symmetry: Auto-swap based on "Priority"
        ItemStack target = item1.clone();
        ItemStack sacrifice = item2.clone();

        int tPriority = getItemPriority(target);
        int sPriority = getItemPriority(sacrifice);

        // If sacrifice has higher priority, OR if they are the same (like two books)
        // but sacrifice is "heavier" (more enchants/higher levels), swap for
        // consistency.
        boolean shouldSwap = sPriority > tPriority;
        if (sPriority == tPriority && sPriority > 0) {
            // Tie-break for books or same-tier items: use total level sum to be
            // deterministic
            if (getEnchantLevelSum(sacrifice) > getEnchantLevelSum(target)) {
                shouldSwap = true;
            }
        }

        if (shouldSwap) {
            ItemStack temp = target;
            target = sacrifice;
            sacrifice = temp;
        }

        ItemStack result = calculateResult(target, sacrifice);
        int cost = result != null ? calculateTotalCost(item1, item2, result) : 0;

        // Firing API event for external plugins (like CrazyMinions)
        dev.agam.skyblockitems.api.events.SkyBlockAnvilUpdateEvent apiEvent = 
            new dev.agam.skyblockitems.api.events.SkyBlockAnvilUpdateEvent(player, target, sacrifice, result, cost);
        int listeners = dev.agam.skyblockitems.api.events.SkyBlockAnvilUpdateEvent.getHandlerList().getRegisteredListeners().length;
        Bukkit.getPluginManager().callEvent(apiEvent);

        if (apiEvent.isCancelled()) {
            resetResultWithReason("incompatible");
            return;
        }

        result = apiEvent.getResult();
        cost = apiEvent.getCost();
        
        // 2. Classloader-safe Registry Fallback (If event had no listeners or failed)
        if (listeners == 0 || result == null) {
            Object[] data = new Object[]{ target, sacrifice, null, null }; // [Left, Right, Result, Cost]
            for (java.util.function.Consumer<Object[]> handler : dev.agam.skyblockitems.api.AnvilRegistry.getHandlers()) {
                try {
                    handler.accept(data);
                    if (data[2] instanceof ItemStack res) {
                        result = res;
                        if (data[3] instanceof Integer c) cost = c;
                        break;
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (result != null && apiEvent.getResult() != result) {
             org.bukkit.Bukkit.broadcastMessage("§b[Anvil-Debug] Result was modified by a plugin!");
        }

        if (result == null) {
            String reason = "incompatible";
            if (areConflictingInResult(target, sacrifice))
                reason = "conflicts";
            resetResultWithReason(reason);
        } else {
            int maxCost = plugin.getConfig().getInt("anvil.max-repair-cost", 40);
            boolean limitEnabled = plugin.getConfig().getBoolean("anvil.limit-enabled", true);
            boolean bypassOp = plugin.getConfig().getBoolean("anvil.bypass-limits-on-op", true);

            if (limitEnabled && cost >= maxCost && !(player.isOp() && bypassOp)) {
                inventory.setItem(RESULT_SLOT, createPane(Material.BARRIER,
                        plugin.getConfigManager().getMessage("anvil.too-expensive-message", "{max}",
                                String.valueOf(maxCost))));
                updateCombineButton(false, cost, "too-expensive");
                updateIndicators(false);
            } else {
                inventory.setItem(RESULT_SLOT, result);
                updateCombineButton(true, cost, "ready");
                updateIndicators(true);
            }
        }
    }

    private void resetResultWithReason(String reason) {
        inventory.setItem(RESULT_SLOT, createPane(Material.GRAY_DYE,
                plugin.getConfigManager().getMessage("anvil.result-label")));
        updateCombineButton(false, 0, reason);
        updateIndicators(false);
    }

    private boolean areConflictingInResult(ItemStack item1, ItemStack item2) {
        if (isEmpty(item1) || isEmpty(item2))
            return false;

        Map<String, Integer> e1 = getEnchantMap(item1);
        Map<String, Integer> e2 = getEnchantMap(item2);

        for (String id1 : e1.keySet()) {
            for (String id2 : e2.keySet()) {
                if (areConflicting(id1, id2))
                    return true;
            }
        }
        return false;
    }

    private void resetResult() {
        resetResultWithReason("empty");
    }

    private int getItemPriority(ItemStack item) {
        if (item == null || item.getType().isAir())
            return 0;
        Material mat = item.getType();

        // Armor and weapons are top priority
        if (isPrimaryItem(item))
            return 10;
        // Books are second
        if (mat == Material.ENCHANTED_BOOK || mat == Material.BOOK)
            return 5;
        // Everything else (heads, etc)
        return 1;
    }

    private boolean isPrimaryItem(ItemStack item) {
        if (item == null || item.getType().isAir())
            return false;
        Material mat = item.getType();
        String name = mat.name();
        return name.contains("SWORD") || name.contains("AXE") || name.contains("PICKAXE") ||
                name.contains("SHOVEL") || name.contains("HOE") || name.contains("BOW") ||
                name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") ||
                name.contains("BOOTS") || name.contains("TRIDENT") || name.equals("MACE");
    }

    private ItemStack calculateResult(ItemStack target, ItemStack sacrifice) {
        if (isEmpty(target) || isEmpty(sacrifice))
            return null;

        // Strict Matching
        boolean targetIsBook = target.getType() == Material.ENCHANTED_BOOK || target.getType() == Material.BOOK;
        boolean sacrificeIsBook = sacrifice.getType() == Material.ENCHANTED_BOOK
                || sacrifice.getType() == Material.BOOK;

        if (!targetIsBook && !sacrificeIsBook) {
            if (target.getType() != sacrifice.getType())
                return null;
        }

        ItemStack result = target.clone();
        boolean changed = false;

        // 1. Durability Repair (Vanilla logic)
        if (!targetIsBook && !sacrificeIsBook && target.getType() == sacrifice.getType()) {
            ItemMeta targetMeta = result.getItemMeta();
            ItemMeta sacrificeMeta = sacrifice.getItemMeta();

            if (targetMeta instanceof Damageable targetDmg && sacrificeMeta instanceof Damageable sacrificeDmg) {
                int maxDurability = result.getType().getMaxDurability();
                if (maxDurability > 0) {
                    int currentDamage = targetDmg.getDamage();
                    if (currentDamage > 0) {
                        // Amount repaired = (Max - SacrificeDamage) + 12% of Max
                        int repairAmount = (maxDurability - sacrificeDmg.getDamage()) + (int) (maxDurability * 0.12);
                        int newDamage = Math.max(0, currentDamage - repairAmount);

                        if (newDamage < currentDamage) {
                            targetDmg.setDamage(newDamage);
                            result.setItemMeta(targetDmg);
                            changed = true;
                        }
                    }
                }
            }
        }

        Map<String, Integer> targetEnchants = getEnchantMap(target);
        Map<String, Integer> sacrificeEnchants = getEnchantMap(sacrifice);

        for (Map.Entry<String, Integer> entry : sacrificeEnchants.entrySet()) {
            String id = entry.getKey();
            int sacrificeLevel = entry.getValue();

            // Applicability Check
            boolean applicable = targetIsBook || isApplicable(id, target);
            if (!applicable)
                continue;

            // Conflict Check
            for (String targetId : targetEnchants.keySet()) {
                if (id.equalsIgnoreCase(targetId))
                    continue;
                if (areConflicting(id, targetId))
                    return null;
            }

            int currentLevel = targetEnchants.getOrDefault(id, 0);
            int newLevel;

            if (currentLevel == 0) {
                newLevel = sacrificeLevel;
            } else if (currentLevel < sacrificeLevel) {
                newLevel = sacrificeLevel;
            } else if (currentLevel == sacrificeLevel) {
                newLevel = currentLevel + 1;
            } else {
                newLevel = currentLevel;
            }

            // Level Cap
            int max = 10;
            var conf = plugin.getEnchantManager().getEnchant(id);
            if (conf != null)
                max = conf.getMaxLevel();
            else {
                var ce = plugin.getCustomEnchantManager().getEnchant(id);
                if (ce != null)
                    max = ce.getMaxLevel();
            }
            newLevel = Math.min(newLevel, max);

            if (newLevel > currentLevel) {
                targetEnchants.put(id, newLevel);
                changed = true;
            }
        }

        if (!changed)
            return null;

        // Re-apply all enchants using the central logic to ensure attributes/lore are
        // correct
        for (Map.Entry<String, Integer> entry : targetEnchants.entrySet()) {
            plugin.getEnchantManager().applyEnchantment(result, entry.getKey(), entry.getValue());
        }

        // 4. Final Processing (Rarity, Lore Formatting)
        result = plugin.getRarityManager().processItem(result);

        return result;
    }

    private boolean isApplicable(String id, ItemStack item) {
        Set<String> categories = getItemCategories(item);

        var conf = plugin.getEnchantManager().getEnchant(id);
        if (conf != null) {
            for (String cat : conf.getTargets()) {
                if (categories.contains(cat) || cat.equals("GLOBAL"))
                    return true;
            }
            return false;
        }

        var ce = plugin.getCustomEnchantManager().getEnchant(id);
        if (ce != null) {
            for (String cat : ce.getTargets()) {
                if (categories.contains(cat) || cat.equals("GLOBAL"))
                    return true;
            }
            return false;
        }

        org.bukkit.enchantments.Enchantment v = org.bukkit.enchantments.Enchantment
                .getByKey(org.bukkit.NamespacedKey.minecraft(id));
        return v != null && v.canEnchantItem(item);
    }

    private void handleCombine() {
        ItemStack result = inventory.getItem(RESULT_SLOT);
        if (isEmpty(result) || result.getType().name().contains("GLASS") || result.getType() == Material.BARRIER
                || result.getType() == Material.GRAY_DYE)
            return;

        int cost = calculateTotalCost(inventory.getItem(ITEM_1_SLOT), inventory.getItem(ITEM_2_SLOT), result);
        int maxCost = plugin.getConfig().getInt("anvil.max-repair-cost", 40);
        boolean limitEnabled = plugin.getConfig().getBoolean("anvil.limit-enabled", true);
        boolean bypassOp = plugin.getConfig().getBoolean("anvil.bypass-limits-on-op", true);

        if (limitEnabled && cost >= maxCost && !(player.isOp() && bypassOp)) {
            player.sendMessage(plugin.getConfigManager().getMessage("anvil.too-expensive-message"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        if (player.getLevel() < cost && !(player.isOp() && bypassOp)) {
            player.sendMessage(plugin.getConfigManager().getMessage("anvil.not-enough-xp"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        if (!(player.isOp() && bypassOp))
            player.setLevel(Math.max(0, player.getLevel() - cost));

        // Update result's RepairCost NBT (Prior Work Penalty)
        updateRepairCost(result, inventory.getItem(ITEM_1_SLOT), inventory.getItem(ITEM_2_SLOT));

        // Clear inputs
        inventory.setItem(ITEM_1_SLOT, null);
        inventory.setItem(ITEM_2_SLOT, null);

        // Give item to player directly (ensures it doesn't vanish)
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        // Success feedback
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
        player.sendMessage(plugin.getConfigManager().getMessage("anvil.combine-success"));

        // AuraSkills XP Integration
        if (plugin.isAuraSkillsEnabled()) {
            // Balanced XP: Base(5) + (XP_Cost / 2)
            double xpAmount = 5.0 + (cost / 2.0);
            plugin.getAuraSkillsHook().addXP(player, "enchanting", xpAmount);
        }

        // Ensure Rarity and Spacing are applied
        plugin.getRarityManager().processItem(result);

        // Reset GUI state safely
        updateResult();
    }

    private Map<String, Integer> getEnchantMap(ItemStack item) {
        if (isEmpty(item) || !item.hasItemMeta())
            return new HashMap<>();

        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        Map<String, Integer> enchants = plugin.getEnchantManager().parseLore(lore);

        // Also capture vanilla enchants from the item itself
        item.getEnchantments().forEach((e, l) -> enchants.put(e.getKey().getKey().toLowerCase(), l));

        // CRITICAL: Also capture stored enchants from Enchanted Books
        if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta esm) {
            esm.getStoredEnchants().forEach((e, l) -> enchants.put(e.getKey().getKey().toLowerCase(), l));
        }

        return enchants;
    }

    private boolean areConflicting(String id1, String id2) {
        if (id1.equalsIgnoreCase(id2))
            return false;

        // Custom conflicts
        var ce1 = plugin.getCustomEnchantManager().getEnchant(id1);
        if (ce1 != null && ce1.getConflicts().contains(id2))
            return true;
        var ce2 = plugin.getCustomEnchantManager().getEnchant(id2);
        if (ce2 != null && ce2.getConflicts().contains(id1))
            return true;

        // Vanilla conflicts
        org.bukkit.enchantments.Enchantment e1 = org.bukkit.enchantments.Enchantment
                .getByKey(org.bukkit.NamespacedKey.minecraft(id1));
        org.bukkit.enchantments.Enchantment e2 = org.bukkit.enchantments.Enchantment
                .getByKey(org.bukkit.NamespacedKey.minecraft(id2));
        if (e1 != null && e2 != null) {
            return e1.conflictsWith(e2);
        }

        // Special case for Fortune/Silk Touch if not covered
        boolean f1 = id1.contains("fortune") || id1.contains("silk_touch");
        boolean f2 = id2.contains("fortune") || id2.contains("silk_touch");
        if (f1 && f2 && !id1.equalsIgnoreCase(id2))
            return true;

        return false;
    }

    private List<String> rebuildLore(List<String> original, Map<String, Integer> enchants) {
        Set<String> enchantNames = new HashSet<>();
        for (org.bukkit.enchantments.Enchantment e : org.bukkit.enchantments.Enchantment.values()) {
            enchantNames.add(e.getKey().getKey().replace("_", " ").toLowerCase());
        }
        plugin.getEnchantManager().getEnchants().values().forEach(
                c -> enchantNames.add(ChatColor.stripColor(ColorUtils.colorize(c.getDisplayName())).toLowerCase()));
        plugin.getCustomEnchantManager().getAllEnchants().forEach(
                ce -> enchantNames.add(ChatColor.stripColor(ColorUtils.colorize(ce.getDisplayName())).toLowerCase()));

        List<String> nonEnchantLines = new ArrayList<>();
        if (original != null) {
            for (String line : original) {
                String clean = ChatColor.stripColor(line).toLowerCase();
                boolean hasEnchant = false;
                for (String name : enchantNames) {
                    if (clean.contains(name)) {
                        hasEnchant = true;
                        break;
                    }
                }
                if (!hasEnchant)
                    nonEnchantLines.add(line);
            }
        }

        List<String> result = new ArrayList<>();
        List<String> entries = new ArrayList<>();
        List<String> sortedKeys = new ArrayList<>(enchants.keySet());
        Collections.sort(sortedKeys);

        for (String id : sortedKeys) {
            int lvl = enchants.get(id);
            String displayName = null;
            var config = plugin.getEnchantManager().getEnchant(id);
            if (config != null)
                displayName = config.getDisplayName();
            else {
                var ce = plugin.getCustomEnchantManager().getEnchant(id);
                if (ce != null)
                    displayName = ce.getDisplayName();
            }
            if (displayName == null)
                displayName = "&7" + id.substring(0, 1).toUpperCase() + id.substring(1).replace("_", " ");
            entries.add(ChatColor.GRAY + ChatColor.stripColor(ColorUtils.colorize(displayName))
                    + (toRoman(lvl, id).isEmpty() ? "" : " " + toRoman(lvl, id)));
        }

        StringBuilder currentLine = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0 && i % 3 != 0)
                currentLine.append("§7, ");
            currentLine.append(entries.get(i));
            if ((i + 1) % 3 == 0 || i == entries.size() - 1) {
                result.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
        }
        result.addAll(nonEnchantLines);
        return result;
    }

    private int fromRoman(String r) {
        return switch (r.toUpperCase()) {
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

    private String toRoman(int n, String id) {
        var conf = plugin.getEnchantManager().getEnchant(id);
        if (conf != null && conf.getMaxLevel() <= 1 && n == 1)
            return "";

        var ce = plugin.getCustomEnchantManager().getEnchant(id);
        if (ce != null && ce.getMaxLevel() <= 1 && n == 1)
            return "";

        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> "";
        };
    }

    private Set<String> getItemCategories(ItemStack item) {
        Set<String> categories = new HashSet<>();
        if (item == null)
            return categories;
        String type = item.getType().name();
        if (type.endsWith("_HELMET")) {
            categories.add("ARMOR");
            categories.add("HELMET");
        }
        if (type.endsWith("_CHESTPLATE")) {
            categories.add("ARMOR");
            categories.add("CHESTPLATE");
        }
        if (type.endsWith("_LEGGINGS")) {
            categories.add("ARMOR");
            categories.add("LEGGINGS");
        }
        if (type.endsWith("_BOOTS")) {
            categories.add("ARMOR");
            categories.add("BOOTS");
        }
        if (type.endsWith("_SWORD"))
            categories.add("SWORD");
        if (type.endsWith("_AXE")) {
            categories.add("AXE");
            categories.add("TOOL");
        }
        if (type.endsWith("_PICKAXE"))
            categories.add("TOOL");
        if (type.endsWith("_SHOVEL") || type.endsWith("_SPADE"))
            categories.add("TOOL");
        if (type.endsWith("_HOE"))
            categories.add("TOOL");
        if (type.contains("BOW"))
            categories.add("BOW");
        if (type.contains("CROSSBOW"))
            categories.add("CROSSBOW");
        if (type.contains("FISHING_ROD"))
            categories.add("FISHING_ROD");
        if (type.contains("TRIDENT"))
            categories.add("TRIDENT");
        if (type.equals("MACE"))
            categories.add("MACE");
        return categories;
    }

    private int getEnchantLevelSum(ItemStack item) {
        if (item == null || item.getItemMeta() == null)
            return 0;
        Map<String, Integer> enchants = getEnchantMap(item);
        return enchants.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int calculateTotalCost(ItemStack item1, ItemStack item2, ItemStack result) {
        if (isEmpty(item1) || isEmpty(item2) || isEmpty(result))
            return 0;

        int cost = 0;

        // 1. Prior Work Penalty
        cost += plugin.getEnchantManager().getPriorWorkPenalty(item1);
        cost += plugin.getEnchantManager().getPriorWorkPenalty(item2);

        // 2. Repair Cost (Vanilla: 2 levels if repaired)
        if (item1.getItemMeta() instanceof Damageable d1 && result.getItemMeta() instanceof Damageable dr) {
            if (dr.getDamage() < d1.getDamage()) {
                cost += 2;
            }
        }

        // 2. Enchantment Merging Cost
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();

        Map<String, Integer> targetEnchants = getEnchantMap(item1);
        Map<String, Integer> sacrificeEnchants = getEnchantMap(item2);
        Map<String, Integer> resultEnchants = getEnchantMap(result);

        for (Map.Entry<String, Integer> entry : sacrificeEnchants.entrySet()) {
            String id = entry.getKey();
            int sacrificeLevel = entry.getValue();
            int resultLevel = resultEnchants.getOrDefault(id, 0);
            int targetLevel = targetEnchants.getOrDefault(id, 0);

            // If it's applicable and resulted in a change/keeping
            if (resultLevel > 0) {
                int multiplier = getAnvilMultiplier(id);

                // Vanilla mechanic:
                // - If new: level * multiplier
                // - If upgraded: new_level * multiplier
                // - If same: level * multiplier (yes, even if not upgraded, if it's on
                // sacrifice and fits)
                // - If book -> sword: multiplier is usually lower? No, vanilla uses same
                // multiplier.

                cost += resultLevel * multiplier;
            }
        }
        return Math.max(1, cost);
    }

    private void updateRepairCost(ItemStack result, ItemStack item1, ItemStack item2) {
        int p1 = plugin.getEnchantManager().getPriorWorkPenalty(item1);
        int p2 = plugin.getEnchantManager().getPriorWorkPenalty(item2);

        ItemMeta meta = result.getItemMeta();
        if (meta instanceof org.bukkit.inventory.meta.Repairable r) {
            r.setRepairCost(Math.max(p1, p2) * 2 + 1);
            result.setItemMeta(meta);
        }
    }

    private int getAnvilMultiplier(String id) {
        // Vanilla Multipliers (Common)
        switch (id.toLowerCase()) {
            case "sharpness":
            case "protection":
            case "efficiency":
            case "power":
            case "unbreaking":
                return 1;
            case "fire_aspect":
            case "knockback":
            case "thorns":
            case "respiration":
                return 2;
            case "fortune":
            case "looting":
            case "smite":
            case "feather_falling":
                return 4;
            case "mending":
            case "silk_touch":
            case "infinity":
                return 8;
        }

        // Custom Enchants
        var conf = plugin.getEnchantManager().getEnchant(id);
        if (conf != null)
            return conf.getAnvilMultiplier();

        return 2; // Default
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
