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
    private Inventory inventory;
    private final Map<String, Double> currentStats;
    private final ReforgeStatEditorGUI parentGUI;

    // Map of stat ID to stat display name
    private final Map<String, String> availableStats;

    // Pagination
    private int currentPage = 0;
    private static final int STATS_PER_PAGE = 45;

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
     * Loads allowed stats from MMOItems API based on user-approved list.
     */
    private void loadMMOItemsStats() {
        // User's allowed stat IDs (normalized to internal MMOItems IDs)
        Set<String> allowedStats = new HashSet<>(Arrays.asList(
                "ATTACK_DAMAGE", "ATTACK_SPEED", "CRITICAL_STRIKE_CHANCE", "CRITICAL_STRIKE_POWER",
                "SKILL_CRITICAL_STRIKE_CHANCE", "SKILL_CRITICAL_STRIKE_POWER", "PVE_DAMAGE", "PVP_DAMAGE",
                "WEAPON_DAMAGE", "SKILL_DAMAGE", "PROJECTILE_DAMAGE", "MAGIC_DAMAGE", "PHYSICAL_DAMAGE",
                "DEFENSE", "FALL_DAMAGE_REDUCTION", "FIRE_DAMAGE_REDUCTION", "PVE_DAMAGE_REDUCTION",
                "PVP_DAMAGE_REDUCTION", "UNDEAD_DAMAGE", "LIFESTEAL",
                "ARMOR_TOUGHNESS", "MAX_HEALTH", "KNOCKBACK_RESISTANCE", "MOVEMENT_SPEED",
                "JUMP_STRENGTH", "MINING_EFFICIENCY", "MOVEMENT_EFFICIENCY", "OXYGEN_BONUS",
                "SNEAKING_SPEED", "WATER_MOVEMENT_SPEED", "AUTOSMELT"));

        try {
            Collection<ItemStat<?, ?>> stats = MMOItems.plugin.getStats().getAll();
            for (ItemStat<?, ?> stat : stats) {
                String statId = stat.getId().toUpperCase().replace("-", "_");
                if (allowedStats.contains(statId)) {
                    availableStats.put(stat.getId(), stat.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load MMOItems stats: " + e.getMessage());
            // Basic fallbacks if MMOItems fails
            availableStats.put("ATTACK_DAMAGE", "Attack Damage");
            availableStats.put("MAX_HEALTH", "Max Health");
            availableStats.put("DEFENSE", "Defense");
        }
    }

    private Material getStatMaterial(String statId) {
        String id = statId.toUpperCase().replace("-", "_");
        if (id.contains("DAMAGE") && !id.contains("REDUCTION"))
            return Material.IRON_SWORD;
        if (id.contains("SPEED"))
            return Material.FEATHER;
        if (id.contains("CRITICAL"))
            return Material.BLAZE_POWDER;
        if (id.contains("HEALTH"))
            return Material.APPLE;
        if (id.contains("DEFENSE") || id.contains("ARMOR"))
            return Material.IRON_CHESTPLATE;
        if (id.contains("REDUCTION"))
            return Material.SHIELD;
        if (id.contains("MINING") || id.contains("EFFICIENCY"))
            return Material.IRON_PICKAXE;
        if (id.contains("MANA"))
            return Material.LAPIS_LAZULI;
        if (id.contains("LIFESTEAL"))
            return Material.REDSTONE;
        if (id.contains("AUTOSMELT"))
            return Material.LAVA_BUCKET;
        if (id.contains("JUMP"))
            return Material.RABBIT_FOOT;
        if (id.contains("OXYGEN") || id.contains("WATER"))
            return Material.WATER_BUCKET;

        return Material.PAPER;
    }

    @Override
    public void open() {
        player.openInventory(inventory);
    }

    private void setupGUI() {
        inventory.clear();

        // Calculate pagination
        List<String> statIds = new ArrayList<>(availableStats.keySet());
        int totalPages = (int) Math.ceil(statIds.size() / (double) STATS_PER_PAGE);
        int startIndex = currentPage * STATS_PER_PAGE;
        int endIndex = Math.min(startIndex + STATS_PER_PAGE, statIds.size());

        // Display stats for current page
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String statId = statIds.get(i);
            String statName = availableStats.get(statId);
            boolean hasCurrentValue = currentStats.containsKey(statId);

            ItemStack item = new ItemStack(getStatMaterial(statId));
            ItemMeta meta = item.getItemMeta();

            // Format name
            String finalStatName = plugin.getReforgeManager().formatStatName(statId);
            String displayName = hasCurrentValue ? "<#2ecc71>✔ " : "<#636e72>◆ ";
            displayName += "<#ffeaa7>" + finalStatName;
            meta.setDisplayName(ColorUtils.colorize(displayName));

            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.stat-id-label")
                    .replace("{id}", statId));

            if (hasCurrentValue) {
                lore.add(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.current-value")
                        .replace("{value}", String.valueOf(currentStats.get(statId))));
            } else {
                lore.add(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.not-set"));
            }

            lore.add("");
            lore.add(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.click-add"));

            meta.setLore(ColorUtils.colorizeList(lore));
            item.setItemMeta(meta);

            inventory.setItem(slot++, item);
        }

        // Bottom row - Navigation
        // Previous page button (slot 45)
        if (currentPage > 0) {
            inventory.setItem(45, createButton(Material.RED_STAINED_GLASS_PANE,
                    plugin.getConfigManager().getMessage("reforge.editor.stat-selector.page-prev"),
                    Arrays.asList(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.page-indicator")
                            .replace("{current}", String.valueOf(currentPage))
                            .replace("{total}", String.valueOf(totalPages)))));
        }

        // Page indicator (slot 49)
        Material[] pageColors = {
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE
        };
        Material pageIndicatorMat = pageColors[currentPage % pageColors.length];
        inventory.setItem(49, createButton(pageIndicatorMat,
                plugin.getConfigManager().getMessage("reforge.editor.stat-selector.page-indicator")
                        .replace("{current}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages)),
                Arrays.asList(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.page-indicator-lore")
                        .replace("{total}", String.valueOf(statIds.size())))));

        // Next page button (slot 53)
        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createButton(Material.GREEN_STAINED_GLASS_PANE,
                    plugin.getConfigManager().getMessage("reforge.editor.stat-selector.page-next"),
                    Arrays.asList(plugin.getConfigManager().getMessage("reforge.editor.stat-selector.page-indicator")
                            .replace("{current}", String.valueOf(currentPage + 2))
                            .replace("{total}", String.valueOf(totalPages)))));
        }

        // Back button (slot 48)
        inventory.setItem(48, createButton(Material.ARROW,
                plugin.getConfigManager().getMessage("gui.items.back.name"),
                plugin.getConfigManager().getMessageList("gui.items.back.lore")));

        // Filler
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.list.filler-material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
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

        // Previous page
        if (slot == 45 && currentPage > 0) {
            currentPage--;
            setupGUI();
            return;
        }

        // Next page
        if (slot == 53) {
            List<String> statIds = new ArrayList<>(availableStats.keySet());
            int totalPages = (int) Math.ceil(statIds.size() / (double) STATS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                currentPage++;
                setupGUI();
            }
            return;
        }

        // Back button
        if (slot == 48) {
            parentGUI.reopen();
            return;
        }

        // Stat selection
        if (slot < 45) {
            List<String> statIds = new ArrayList<>(availableStats.keySet());
            int absoluteIndex = (currentPage * STATS_PER_PAGE) + slot;
            if (absoluteIndex >= statIds.size())
                return;

            String selectedStatId = statIds.get(absoluteIndex);
            String selectedStatName = availableStats.get(selectedStatId);

            // Prompt for value
            player.closeInventory();
            String promptMsg = plugin.getConfigManager().getMessage("reforge.editor.stat-selector.value-prompt")
                    .replace("{name}", selectedStatName)
                    .replace("{id}", selectedStatId);
            player.sendMessage(ColorUtils.colorize(promptMsg));

            plugin.getChatInputManager().awaitInput(player, input -> {
                try {
                    double value = Double.parseDouble(input);
                    String systemStatId = selectedStatId;
                    // Ensure internal stat ID format
                    if (!selectedStatId.toLowerCase().startsWith("mmoitems_")) {
                        systemStatId = "mmoitems_" + selectedStatId.toLowerCase();
                    }
                    currentStats.put(systemStatId, value);
                    String successMsg = plugin.getConfigManager()
                            .getMessage("reforge.editor.stat-selector.added-success")
                            .replace("{name}", selectedStatName)
                            .replace("{value}", String.valueOf(value));
                    player.sendMessage(ColorUtils.colorize(successMsg));
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
