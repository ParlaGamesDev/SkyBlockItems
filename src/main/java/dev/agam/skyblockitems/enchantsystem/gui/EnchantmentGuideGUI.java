package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A comprehensive guide for all enchantments in the plugin.
 */
public class EnchantmentGuideGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private final BaseGUI parent;
    private int page = 0;
    private String searchQuery = "";
    private SortOrder sortOrder = SortOrder.ALPHABETICAL_AZ;

    private enum SortOrder {
        ALPHABETICAL_AZ,
        ALPHABETICAL_ZA,
        LEVEL_HIGH_TO_LOW,
        LEVEL_LOW_TO_HIGH;
    }

    public EnchantmentGuideGUI(SkyBlockItems plugin, Player player, BaseGUI parent) {
        this.plugin = plugin;
        this.player = player;
        this.parent = parent;
        String title = plugin.getConfigManager().getMessage("guide.gui-title");
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    @Override
    public void open() {
        updateInventory();
        player.openInventory(inventory);
    }

    private void updateInventory() {
        inventory.clear();

        // Gather all enchantments
        List<EnchantConfig> allEnchants = new ArrayList<>();
        plugin.getEnchantManager().getEnchants().values().stream()
                .filter(EnchantConfig::isEnabled)
                .forEach(allEnchants::add);
        plugin.getCustomEnchantManager().getAllEnchants().stream()
                .filter(CustomEnchant::isEnabled)
                .map(CustomEnchant::toEnchantConfig)
                .forEach(allEnchants::add);

        // Apply Search
        if (!searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            allEnchants = allEnchants.stream()
                    .filter(e -> ChatColor.stripColor(ColorUtils.colorize(e.getDisplayName())).toLowerCase()
                            .contains(query))
                    .collect(Collectors.toList());
        }

        // Apply Sorting
        switch (sortOrder) {
            case ALPHABETICAL_AZ:
                allEnchants.sort((e1, e2) -> ChatColor.stripColor(ColorUtils.colorize(e1.getDisplayName()))
                        .compareTo(ChatColor.stripColor(ColorUtils.colorize(e2.getDisplayName()))));
                break;
            case ALPHABETICAL_ZA:
                allEnchants.sort((e1, e2) -> ChatColor.stripColor(ColorUtils.colorize(e2.getDisplayName()))
                        .compareTo(ChatColor.stripColor(ColorUtils.colorize(e1.getDisplayName()))));
                break;
            case LEVEL_HIGH_TO_LOW:
                allEnchants.sort(
                        (e1, e2) -> Integer.compare(e2.getRequiredEnchantingLevel(), e1.getRequiredEnchantingLevel()));
                break;
            case LEVEL_LOW_TO_HIGH:
                allEnchants.sort(
                        (e1, e2) -> Integer.compare(e1.getRequiredEnchantingLevel(), e2.getRequiredEnchantingLevel()));
                break;
        }

        // Pagination
        int slotsPerPage = 28; // Rows 2,3,4,5 inner slots (7x4)
        List<Integer> availableSlots = new ArrayList<>();
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                availableSlots.add(row * 9 + col);
            }
        }

        int totalEnchants = allEnchants.size();
        int totalPages = (int) Math.ceil((double) totalEnchants / slotsPerPage);
        if (page >= totalPages && totalPages > 0)
            page = totalPages - 1;

        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, totalEnchants);

        for (int i = startIndex; i < endIndex; i++) {
            inventory.setItem(availableSlots.get(i - startIndex), createGuideIcon(allEnchants.get(i)));
        }

        // Navigation
        if (page > 0) {
            inventory.setItem(47, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.prev-page"), Material.ARROW));
        }
        if (page < totalPages - 1) {
            inventory.setItem(51, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.next-page"), Material.ARROW));
        }

        // Back button (Standardized: slot 49, Arrow material)
        inventory.setItem(49, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));

        // Search button (Slot 48)
        ItemStack searchItem = ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.guide-search"), Material.OAK_SIGN);
        ItemMeta searchMeta = searchItem.getItemMeta();
        if (searchMeta != null) {
            String searchName = searchQuery.isEmpty() ? plugin.getConfigManager().getMessage("guide.search.title")
                    : plugin.getConfigManager().getMessage("guide.search.current", "{query}", searchQuery);
            searchMeta.setDisplayName(ColorUtils.colorize(searchName));
            searchItem.setItemMeta(searchMeta);
        }
        inventory.setItem(48, searchItem);

        // Sort Toggle (Slot 50)
        ItemStack sortItem = ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.sort"), Material.HOPPER);
        ItemMeta sortMeta = sortItem.getItemMeta();
        if (sortMeta != null) {
            List<String> sortLore = new ArrayList<>();
            sortLore.add("");
            for (SortOrder order : SortOrder.values()) {
                boolean selected = (order == sortOrder);
                String prefix = selected ? "&a» " : "&8» ";
                String color = selected ? "&a" : "&7";
                String orderName = switch (order) {
                    case ALPHABETICAL_AZ -> plugin.getConfigManager().getMessageRaw("guide.sort.modes.alphabetical-az");
                    case ALPHABETICAL_ZA -> plugin.getConfigManager().getMessageRaw("guide.sort.modes.alphabetical-za");
                    case LEVEL_HIGH_TO_LOW ->
                        plugin.getConfigManager().getMessageRaw("guide.sort.modes.level-high-to-low");
                    case LEVEL_LOW_TO_HIGH ->
                        plugin.getConfigManager().getMessageRaw("guide.sort.modes.level-low-to-high");
                };
                sortLore.add(ColorUtils.colorize(prefix + color + orderName));
            }
            sortLore.add("");
            sortLore.add(plugin.getConfigManager().getMessage("guide.sort.left-click"));
            sortLore.add(plugin.getConfigManager().getMessage("guide.sort.right-click"));
            sortMeta.setLore(sortLore);
            sortItem.setItemMeta(sortMeta);
        }
        inventory.setItem(50, sortItem);

        // Fill remaining empty slots with configured glass pane
        Material fillerMat = Material.getMaterial(
                plugin.getConfig().getString("gui.guide.filler-material", "LIGHT_BLUE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, filler);
            }
        }
    }

    private ItemStack createGuideIcon(EnchantConfig enchant) {
        ItemStack item = new ItemStack(enchant.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        String romanMax = toRoman(enchant.getMaxLevel());
        meta.setDisplayName(ColorUtils.colorize("&a" + enchant.getDisplayName() + " " + romanMax));

        List<String> lore = new ArrayList<>();
        // Description
        String desc = getDetailedDescription(enchant);
        if (desc.contains("\n")) {
            for (String line : desc.split("\n")) {
                lore.add(ColorUtils.colorize("&7" + line.trim()));
            }
        } else {
            lore.add(ColorUtils.colorize("&7" + desc));
        }
        lore.add("");

        // Applied To
        lore.add(plugin.getConfigManager().getMessage("guide.lore.applied-to"));
        for (String target : enchant.getTargets()) {
            lore.add(plugin.getConfigManager().getMessage("guide.lore.list-format", "{value}",
                    formatTargetName(target)));
        }
        lore.add("");

        // Requirements
        lore.add(plugin.getConfigManager().getMessage("guide.lore.requirements"));
        int required = enchant.getRequiredEnchantingLevel();
        lore.add(plugin.getConfigManager().getMessage("guide.lore.req-level", "{level}", String.valueOf(required)));

        // Conflicts
        List<String> conflicts = enchant.getConflicts();
        if (!conflicts.isEmpty()) {
            lore.add("");
            lore.add(plugin.getConfigManager().getMessage("guide.lore.conflicts"));
            for (String cid : conflicts) {
                String dname = plugin.getEnchantManager().getDisplayNameForId(cid);
                lore.add(plugin.getConfigManager().getMessage("guide.lore.list-format", "{value}",
                        ChatColor.stripColor(ColorUtils.colorize(dname))));
            }
        }

        // Status - Bottom
        if (plugin.isAuraSkillsEnabled()) {
            int playerLevel = plugin.getAuraSkillsHook().getEnchantingLevel(player);
            String statusKey = playerLevel >= required ? "guide.lore.status-unlocked" : "guide.lore.status-locked";
            String statusText = plugin.getConfigManager().getMessage(statusKey);

            lore.add("");
            lore.add(plugin.getConfigManager().getMessage("guide.lore.status", "{status}", statusText));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private String getDetailedDescription(EnchantConfig enchant) {
        String id = enchant.getId().toLowerCase();
        String path = "enchant-descriptions." + id;
        return plugin.getConfigManager().getMessage(path);
    }

    private String formatTargetName(String target) {
        if (target == null || target.isEmpty())
            return "";
        String lower = target.toLowerCase();
        String path = "targets." + lower;
        return plugin.getConfigManager().getMessage(path);
    }

    private String toRoman(int num) {
        return switch (num) {
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
            default -> String.valueOf(num);
        };
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        int slot = event.getSlot();

        // Back
        if (slot == 49) {
            if (parent != null)
                parent.open();
            else
                player.closeInventory();
            return;
        }

        // Navigation
        if (slot == 47 && page > 0) {
            page--;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            updateInventory();
            return;
        }
        if (slot == 51) {
            page++;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            updateInventory();
            return;
        }

        // Sort Toggle
        if (slot == 50) {
            SortOrder[] allModes = SortOrder.values();
            int currentIdx = sortOrder.ordinal();
            if (event.isLeftClick()) {
                sortOrder = allModes[(currentIdx + 1) % allModes.length];
            } else if (event.isRightClick()) {
                sortOrder = allModes[(currentIdx - 1 + allModes.length) % allModes.length];
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            updateInventory();
            return;
        }

        // Search
        if (slot == 48) {
            if (event.isRightClick() && !searchQuery.isEmpty()) {
                searchQuery = "";
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1, 1);
                updateInventory();
                return;
            }

            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getMessage("guide.search.chat-prompt"));
            plugin.getChatInputManager().awaitInput(player, input -> {
                if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("ביטול")) {
                    Bukkit.getScheduler().runTask(plugin, this::open);
                    return;
                }

                // Character limit from config
                int limit = plugin.getConfig().getInt("anvil.search-limit", 25);
                if (input.length() > limit) {
                    input = input.substring(0, limit);
                }

                searchQuery = input;
                Bukkit.getScheduler().runTask(plugin, this::open);
            });
        }
    }

    @Override
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
