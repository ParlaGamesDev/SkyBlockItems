package dev.agam.skyblockitems.reforge.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.gui.BaseGUI;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI for selecting MMOItems stats to add to a reforge.
 * Dynamically loads all available stats from MMOItems API.
 */
public class ReforgeStatSelectorGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<String, Double> currentStats;
    private final ReforgeStatEditorGUI parentGUI;

    // Map of stat ID to stat display name
    private final Map<String, String> availableStats;

    public ReforgeStatSelectorGUI(SkyBlockItems plugin, Player player, Map<String, Double> currentStats,
            ReforgeStatEditorGUI parentGUI) {
        this.plugin = plugin;
        this.player = player;
        this.currentStats = currentStats;
        this.parentGUI = parentGUI;
        this.availableStats = new LinkedHashMap<>();

        loadMMOItemsStats();

        String title = ColorUtils.colorize("<#d63aff>&lבחר סטטיסטיקה");
        this.inventory = Bukkit.createInventory(this, 54, title);
        setupGUI();
    }

    /**
     * Loads all available stats from MMOItems API
     */
    private void loadMMOItemsStats() {
        try {
            plugin.getLogger().info("[DEBUG] ===== Starting to load MMOItems stats =====");

            // Get all registered stats from MMOItems
            Collection<ItemStat<?, ?>> stats = MMOItems.plugin.getStats().getAll();
            plugin.getLogger().info("[DEBUG] MMOItems API returned " + stats.size() + " total stats");

            int count = 0;
            for (ItemStat<?, ?> stat : stats) {
                String statId = stat.getId();
                String statName = stat.getName();

                // Add to available stats
                availableStats.put(statId, statName);

                // Log ALL stats so user can search for the correct IDs
                plugin.getLogger()
                        .info("[DEBUG]   Stat #" + (count + 1) + ": ID='" + statId + "' | Name='" + statName + "'");
                count++;
            }

            plugin.getLogger().info("[DEBUG] ===== Successfully loaded " + availableStats.size() + " stats =====");

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load MMOItems stats: " + e.getMessage());

            // Fallback to common stats if MMOItems API fails
            availableStats.put("ATTACK_DAMAGE", "Attack Damage");
            availableStats.put("ATTACK_SPEED", "Attack Speed");
            availableStats.put("CRITICAL_STRIKE_CHANCE", "Critical Strike Chance");
            availableStats.put("CRITICAL_STRIKE_POWER", "Critical Strike Power");
            availableStats.put("MAX_HEALTH", "Max Health");
            availableStats.put("ARMOR", "Armor");
            availableStats.put("ARMOR_TOUGHNESS", "Armor Toughness");
            availableStats.put("MAX_MANA", "Max Mana");
            availableStats.put("KNOCKBACK_RESISTANCE", "Knockback Resistance");
            availableStats.put("MOVEMENT_SPEED", "Movement Speed");
            availableStats.put("MAGIC_DAMAGE", "Magic Damage");
            availableStats.put("DEFENSE", "Defense");
            availableStats.put("DODGE_RATING", "Dodge Rating");
            availableStats.put("BLOCK_RATING", "Block Rating");
            availableStats.put("BLOCK_POWER", "Block Power");
            availableStats.put("PARRY_RATING", "Parry Rating");
            availableStats.put("COOLDOWN_REDUCTION", "Cooldown Reduction");
            availableStats.put("RANGE", "Range");
            availableStats.put("MANA_REGENERATION", "Mana Regeneration");
            availableStats.put("STAMINA_REGENERATION", "Stamina Regeneration");
            availableStats.put("MAX_STAMINA", "Max Stamina");
            availableStats.put("HEALTH_REGENERATION", "Health Regeneration");
            availableStats.put("ADDITIONAL_EXPERIENCE", "Additional Experience");
            availableStats.put("DURABILITY", "Durability");
            availableStats.put("UNBREAKABLE", "Unbreakable");
        }
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        int slot = 0;
        for (Map.Entry<String, String> entry : availableStats.entrySet()) {
            if (slot >= 45)
                break; // Leave space for bottom row

            String statId = entry.getKey();
            String statName = entry.getValue();
            boolean hasCurrentValue = currentStats.containsKey(statId);

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            // Format name
            String finalStatName = plugin.getReforgeManager().formatStatName(statId);
            String displayName = hasCurrentValue ? "<#2ecc71>✔ " : "<#636e72>◆ ";
            displayName += "<#ffeaa7>" + finalStatName;
            meta.setDisplayName(ColorUtils.colorize(displayName));

            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("<#636e72>ID: <#dfe6e9>" + statId));

            if (hasCurrentValue) {
                lore.add(ColorUtils.colorize("<#636e72>ערך נוכחי: <#2ecc71>+" + currentStats.get(statId)));
            } else {
                lore.add(ColorUtils.colorize("<#636e72>לא מוגדר"));
            }

            lore.add("");
            lore.add(ColorUtils.colorize("<#ffeaa7>▶ לחץ להוספה/עריכה"));

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot++, item);
        }

        // Back button
        inventory.setItem(49, createButton(Material.BARRIER,
                "<#e74c3c>&lחזור",
                Arrays.asList("<#636e72>חזור לעורך סטטיסטיקות")));

        // Filler
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.list.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 45; i < 54; i++) {
            if (i != 49 && (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR)) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        // Back button
        if (slot == 49) {
            parentGUI.reopen();
            return;
        }

        // Stat selection
        if (slot < 45 && clicked.getType() == Material.PAPER) {
            List<String> statIds = new ArrayList<>(availableStats.keySet());
            if (slot >= statIds.size())
                return;

            String selectedStatId = statIds.get(slot);
            String selectedStatName = availableStats.get(selectedStatId);

            // Prompt for value
            player.closeInventory();
            player.sendMessage(ColorUtils.colorize(
                    "<#f39c12>הזן ערך עבור <#ffeaa7>" + selectedStatName + " <#636e72>(" + selectedStatId + "):"));

            plugin.getChatInputManager().awaitInput(player, input -> {
                try {
                    double value = Double.parseDouble(input);
                    String systemStatId = selectedStatId;
                    if (!selectedStatId.startsWith("mmoitems_")) {
                        systemStatId = "mmoitems_" + selectedStatId.toLowerCase();
                    }
                    currentStats.put(systemStatId, value);
                    player.sendMessage(ColorUtils.colorize(
                            "<#2ecc71>✔ נוסף: <#ffeaa7>" + selectedStatName + " <#2ecc71>+" + value));
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtils.colorize(
                            plugin.getConfigManager().getMessage("reforge.editor.invalid-number")));
                }
                open();
            });
        }
    }

    private ItemStack createButton(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtils.colorize(name));
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ColorUtils.colorize(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void reopen() {
        setupGUI();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
