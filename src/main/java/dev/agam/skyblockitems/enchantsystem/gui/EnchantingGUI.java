package dev.agam.skyblockitems.enchantsystem.gui;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.LevelConfig;
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

/**
 * The main enchanting GUI - based on the old plugin's GUI but improved and
 * recoded.
 */
public class EnchantingGUI implements BaseGUI {

    private final SkyBlockItems plugin;
    private final Player player;
    private final Inventory inventory;
    private ItemStack itemToEnchant;
    private final int ITEM_SLOT = 19;
    private final int DECORATIVE_TABLE_SLOT = 28;
    private int page = 0;
    private boolean returningFromLevelSelect = false;
    private boolean isProcessing = false;
    private SortOrder sortOrder = SortOrder.ALPHABETICAL_AZ;

    private enum SortOrder {
        ALPHABETICAL_AZ,
        ALPHABETICAL_ZA,
        LEVEL_HIGH_TO_LOW,
        LEVEL_LOW_TO_HIGH;
    }

    public EnchantingGUI(SkyBlockItems plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        String title = plugin.getConfigManager().getMessage("enchanting.gui-title");
        int size = plugin.getConfig().getInt("gui.main.size", 54);
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public void open() {
        this.returningFromLevelSelect = false;
        player.openInventory(inventory);
        updateEnchantments();
    }

    /**
     * Reopen the GUI after returning from level selection menu.
     */
    public void reopenFromLevelSelection(Player player, ItemStack enchantedItem) {
        this.itemToEnchant = enchantedItem;
        this.returningFromLevelSelect = true;

        inventory.setItem(ITEM_SLOT, enchantedItem);
        player.openInventory(inventory);
        updateEnchantmentsInInventory(inventory);

        returningFromLevelSelect = false;
    }

    private void updateEnchantmentsInInventory(Inventory inv) {
        // Clear all slots except item slot
        for (int i = 0; i < inv.getSize(); i++) {
            if (i != ITEM_SLOT) {
                inv.setItem(i, null);
            }
        }

        // Defined grid slots (5x3)
        List<Integer> availableSlots = Arrays.asList(
                12, 13, 14, 15, 16,
                21, 22, 23, 24, 25,
                30, 31, 32, 33, 34);

        List<EnchantConfig> applicableEnchants = getApplicableEnchants(itemToEnchant);

        int slotsPerPage = availableSlots.size();
        int totalEnchants = applicableEnchants.size();
        int totalPages = (int) Math.ceil((double) totalEnchants / slotsPerPage);

        if (page < 0)
            page = 0;
        if (page >= totalPages && totalPages > 0)
            page = totalPages - 1;

        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, totalEnchants);

        int slotIdx = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIdx >= availableSlots.size())
                break;
            EnchantConfig enchant = applicableEnchants.get(i);
            ItemStack icon = createEnchantIcon(enchant);
            inv.setItem(availableSlots.get(slotIdx), icon);
            slotIdx++;
        }

        if (page > 0) {
            inv.setItem(48, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.prev-page"), Material.ARROW));
        }
        if (page < totalPages - 1) {
            inv.setItem(52, ColorUtils.getItemFromConfig(
                    plugin.getConfig().getConfigurationSection("gui.items.next-page"), Material.ARROW));
        }

        // Decorative Table Icon
        inv.setItem(DECORATIVE_TABLE_SLOT, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.enchant-table-decorative"),
                Material.ENCHANTING_TABLE));

        // Navigation Row
        // Sort Toggle Button (Slot 51)
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
        inv.setItem(51, sortItem);

        // Back Button (Standardized: slot 50, Arrow material)
        inventory.setItem(50, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.back"), Material.ARROW));

        // Enchantment Guide Button (Slot 49)
        inv.setItem(49, ColorUtils.getItemFromConfig(
                plugin.getConfig().getConfigurationSection("gui.items.guide-open"), Material.BOOK));

        // Fill remaining empty slots with configured glass pane
        Material fillerMat = Material
                .getMaterial(plugin.getConfig().getString("gui.main.filler-material", "PURPLE_STAINED_GLASS_PANE"));
        ItemStack filler = ColorUtils.createFillerItem(fillerMat);

        // List of slots that should NEVER have glass (item slot and enchantment grid)
        List<Integer> functionalSlots = new ArrayList<>(availableSlots);
        functionalSlots.add(ITEM_SLOT);

        for (int i = 0; i < inv.getSize(); i++) {
            if (functionalSlots.contains(i))
                continue;

            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, filler);
            }
        }
    }

    private void updateEnchantments() {
        updateEnchantmentsInInventory(inventory);
    }

    private ItemStack createEnchantIcon(EnchantConfig enchant) {
        int requiredLevel = enchant.getRequiredEnchantingLevel();
        int playerLevel = 0;
        boolean locked = false;

        if (plugin.isAuraSkillsEnabled() && requiredLevel > 0) {
            playerLevel = plugin.getAuraSkillsHook().getEnchantingLevel(player);
            if (playerLevel < requiredLevel)
                locked = true;
        }

        String conflictWith = plugin.getEnchantManager().getConflict(itemToEnchant, enchant);
        boolean conflicted = conflictWith != null;

        if (conflicted || locked) {
            return createLockedIcon(enchant, playerLevel, requiredLevel, conflictWith);
        }

        ItemStack icon = new ItemStack(enchant.getMaterial());
        ItemMeta meta = icon.getItemMeta();

        meta.setDisplayName(ColorUtils.colorize("&a" + enchant.getDisplayName()));

        List<String> lore = new ArrayList<>();
        String desc = enchant.getDescription();
        if (desc.contains("\n")) {
            for (String line : desc.split("\n")) {
                lore.add(ColorUtils.colorize("&7" + line.trim()));
            }
        } else {
            lore.add(ColorUtils.colorize("&7" + desc));
        }
        lore.add("");

        // Check if item already has this enchantment
        int currentLevel = 0;
        if (itemToEnchant != null && itemToEnchant.hasItemMeta() && itemToEnchant.getItemMeta().hasLore()) {
            Map<String, Integer> current = plugin.getEnchantManager().parseLore(itemToEnchant.getItemMeta().getLore());
            currentLevel = current.getOrDefault(enchant.getId().toLowerCase(), 0);
        }
        boolean hasEnchant = currentLevel > 0;
        boolean isMaxed = currentLevel >= enchant.getMaxLevel();

        // Only show required level if player hasn't unlocked it yet OR doesn't have it on the item
        // Actually user said: "after opening the charm to lower the 'required charm level' ... you don't need this at all after a person opens"
        // This means if playerLevel >= requiredLevel, hide it.
        int playerSkillLevel = plugin.isAuraSkillsEnabled()
                ? plugin.getAuraSkillsHook().getEnchantingLevel(player)
                : 100;

        if (enchant.getRequiredEnchantingLevel() > playerSkillLevel && !hasEnchant) {
            lore.add(plugin.getConfigManager().getMessage("enchanting.required-level", "{level}",
                    String.valueOf(enchant.getRequiredEnchantingLevel())));
        }
        lore.add(plugin.getConfigManager().getMessage("enchanting.max-level", "{max}",
                String.valueOf(enchant.getMaxLevel())));
        lore.add("");

        if (isMaxed) {
            lore.add(plugin.getConfigManager().getMessage("enchanting.already-maxed"));
        }

        if (!isMaxed) {
            if (enchant.getMaxLevel() > 1) {
                lore.add(plugin.getConfigManager().getMessage("enchanting.click-to-select-level"));
            } else if (!hasEnchant) {
                lore.add(plugin.getConfigManager().getMessage("enchanting.level-select.click-to-enchant"));
            }
        }
        meta.setLore(lore);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createLockedIcon(EnchantConfig enchant, int playerLevel, int requiredLevel, String conflictWith) {
        String matStr = plugin.getConfigManager().getMessageRaw("enchanting.locked-enchant.material");
        Material mat = Material.getMaterial(matStr);
        ItemStack icon = new ItemStack(mat != null ? mat : Material.BARRIER);
        ItemMeta meta = icon.getItemMeta();

        String name = ColorUtils.colorize("&c" + ChatColor.stripColor(ColorUtils.colorize(enchant.getDisplayName())));
        meta.setDisplayName(name);

        List<String> lore = new ArrayList<>();

        if (conflictWith != null) {
            String conflictMsg = plugin.getConfigManager().getMessage("enchanting.conflict-lore", "{enchant}",
                    conflictWith);
            lore.add(conflictMsg);
        } else if (requiredLevel > playerLevel) {
            String lockMsg = plugin.getConfigManager().getMessage("enchanting.need-enchanting-unlock", "{level}",
                    String.valueOf(requiredLevel));
            lore.add(lockMsg);
        }

        for (String line : plugin.getConfigManager().getMessages()
                .getStringList("enchanting.locked-enchant.lore")) {
            lore.add(ColorUtils.colorize(line
                    .replace("{name}", enchant.getDisplayName())
                    .replace("{description}", enchant.getDescription())
                    .replace("{current}", String.valueOf(playerLevel))
                    .replace("{required}", String.valueOf(requiredLevel))
                    .replace("{max}", String.valueOf(enchant.getMaxLevel()))));
        }
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);

        if (isProcessing)
            return;

        // Handle clicking in player inventory (to place item)
        if (event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR) {
                if (plugin.getConfigManager().isBlacklisted(clicked)) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.blacklisted-item"));
                    return;
                }
                if (clicked.getAmount() > 1) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.one-at-a-time"));
                    return;
                }

                // MMOItems Disable Enchanting Check
                if (io.lumine.mythic.lib.api.item.NBTItem.get(clicked).hasTag("MMOITEMS_DISABLE_ENCHANTING")) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.item-disabled-enchanting"));
                    return;
                }

                if (itemToEnchant != null && itemToEnchant.getType() != Material.AIR) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.remove-current-first"));
                    return;
                }
                // Check if any enchants are available for this player
                List<EnchantConfig> allApplicable = getApplicableEnchants(clicked);
                boolean hasAnyUnlocked = false;
                int playerSkillLevel = plugin.isAuraSkillsEnabled()
                        ? plugin.getAuraSkillsHook().getEnchantingLevel(player)
                        : 100;

                for (EnchantConfig enchant : allApplicable) {
                    if (playerSkillLevel >= enchant.getRequiredEnchantingLevel()) {
                        hasAnyUnlocked = true;
                        break;
                    }
                }

                if (!hasAnyUnlocked) {
                    player.sendMessage(plugin.getConfigManager().getMessage("errors.no-enchants-unlocked"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                itemToEnchant = clicked.clone();
                event.setCurrentItem(null);
                inventory.setItem(ITEM_SLOT, itemToEnchant);
                page = 0;
                updateEnchantments();
            }
            return;
        }

        int slot = event.getSlot();

        // Handle item slot click (to take item back)
        if (slot == ITEM_SLOT) {
            if (itemToEnchant != null && itemToEnchant.getType() != Material.AIR) {
                ItemStack toReturn = itemToEnchant;
                itemToEnchant = null;
                inventory.setItem(ITEM_SLOT, null);
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toReturn);
                if (!leftover.isEmpty()) {
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
                page = 0;
                updateEnchantments();
            }
            return;
        }

        // Navigation
        if (slot == 48 && page > 0) {
            page--;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            updateEnchantments();
            return;
        }

        if (slot == 52) {
            page++;
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            updateEnchantments();
            return;
        }

        // Close Button
        if (slot == 50) {
            player.closeInventory();
            return;
        }

        // Sort Toggle
        if (slot == 51) {
            SortOrder[] allModes = SortOrder.values();
            int currentIdx = sortOrder.ordinal();

            if (event.isLeftClick()) {
                sortOrder = allModes[(currentIdx + 1) % allModes.length];
            } else if (event.isRightClick()) {
                sortOrder = allModes[(currentIdx - 1 + allModes.length) % allModes.length];
            }

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            updateEnchantments();
            return;
        }

        // Enchantment Guide Open
        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
            // Return item to player before opening guide
            if (itemToEnchant != null && itemToEnchant.getType() != Material.AIR && itemToEnchant.getAmount() > 0) {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemToEnchant);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                inventory.setItem(ITEM_SLOT, null);
                itemToEnchant = null;
            }
            this.returningFromLevelSelect = true;
            new EnchantmentGuideGUI(plugin, player, this).open();
            return;
        }

        // Handle enchant click - open level selection
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || itemToEnchant == null)
            return;

        handleEnchantClick(clicked);
    }

    private void handleEnchantClick(ItemStack clicked) {
        String clickedName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        // Check normal enchants - Sort by name length descending to catch longer
        // matches first (e.g. Blast Protection before Protection)
        List<EnchantConfig> sortedEnchants = new ArrayList<>(plugin.getEnchantManager().getEnchants().values());
        sortedEnchants.sort((a, b) -> {
            String nameA = ChatColor.stripColor(ColorUtils.colorize(a.getDisplayName()));
            String nameB = ChatColor.stripColor(ColorUtils.colorize(b.getDisplayName()));
            return Integer.compare(nameB.length(), nameA.length());
        });

        for (EnchantConfig enchant : sortedEnchants) {
            String enchantName = ChatColor.stripColor(ColorUtils.colorize(enchant.getDisplayName()));

            if (clickedName.equalsIgnoreCase(enchantName) || clickedName.contains(enchantName)) {
                // Check conflict
                String conflictWith = plugin.getEnchantManager().getConflict(itemToEnchant, enchant);
                if (conflictWith != null) {
                    player.sendMessage(
                            plugin.getConfigManager().getMessage("errors.conflict-error", "{enchant}", conflictWith));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                // Check lock
                if (plugin.isAuraSkillsEnabled() && enchant.getRequiredEnchantingLevel() > 0) {
                    int playerLevel = plugin.getAuraSkillsHook().getEnchantingLevel(player);
                    if (playerLevel < enchant.getRequiredEnchantingLevel()) {
                        player.sendMessage(plugin.getConfigManager().getMessage("enchanting.need-enchanting-unlock")
                                .replace("{level}", String.valueOf(enchant.getRequiredEnchantingLevel())));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return;
                    }
                }

                // Open level selection GUI or apply immediately
                if (enchant.getMaxLevel() <= 1) {
                    applyEnchantmentImmediately(enchant);
                } else {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    this.returningFromLevelSelect = true;
                    new LevelSelectionGUI(plugin, player, itemToEnchant, enchant, this).open();
                }
                return;
            }
        }

        // Check custom enchants - Sort by name length descending
        List<CustomEnchant> sortedCustom = new ArrayList<>(plugin.getCustomEnchantManager().getAllEnchants());
        sortedCustom.sort((a, b) -> {
            String nameA = ChatColor.stripColor(ColorUtils.colorize(a.getDisplayName()));
            String nameB = ChatColor.stripColor(ColorUtils.colorize(b.getDisplayName()));
            return Integer.compare(nameB.length(), nameA.length());
        });

        for (CustomEnchant customEnchant : sortedCustom) {
            String enchantName = ChatColor.stripColor(ColorUtils.colorize(customEnchant.getDisplayName()));

            if (clickedName.equalsIgnoreCase(enchantName) || clickedName.contains(enchantName)) {
                EnchantConfig config = customEnchant.toEnchantConfig();

                // Check conflict
                String conflictWith = plugin.getEnchantManager().getConflict(itemToEnchant, config);
                if (conflictWith != null) {
                    player.sendMessage(
                            plugin.getConfigManager().getMessage("errors.conflict-error", "{enchant}", conflictWith));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    return;
                }

                // Check lock
                if (plugin.isAuraSkillsEnabled() && config.getRequiredEnchantingLevel() > 0) {
                    int playerLevel = plugin.getAuraSkillsHook().getEnchantingLevel(player);
                    if (playerLevel < config.getRequiredEnchantingLevel()) {
                        player.sendMessage(plugin.getConfigManager().getMessage("enchanting.need-enchanting-unlock")
                                .replace("{level}", String.valueOf(config.getRequiredEnchantingLevel())));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                        return;
                    }
                }

                // Open level selection GUI or apply immediately
                if (config.getMaxLevel() <= 1) {
                    applyEnchantmentImmediately(config);
                } else {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    this.returningFromLevelSelect = true;
                    new LevelSelectionGUI(plugin, player, itemToEnchant, config, this).open();
                }
                return;
            }
        }
    }

    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this)
            return;
        event.setCancelled(true);
    }

    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this)
            return;

        // Don't return item if we're opening level selection
        if (returningFromLevelSelect)
            return;

        // Return item to player
        if (itemToEnchant != null && itemToEnchant.getType() != Material.AIR && itemToEnchant.getAmount() > 0) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemToEnchant);
            if (!leftover.isEmpty()) {
                for (ItemStack item : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            inventory.setItem(ITEM_SLOT, null);
            itemToEnchant = null;
        }
    }

    private Set<String> getItemCategories(ItemStack item) {
        Set<String> categories = new HashSet<>();
        if (item == null)
            return categories;

        // MMOItems Support
        io.lumine.mythic.lib.api.item.NBTItem nbtItem = io.lumine.mythic.lib.api.item.NBTItem.get(item);
        if (nbtItem.hasTag("MMOITEMS_ITEM_TYPE")) {
            String mmoType = nbtItem.getString("MMOITEMS_ITEM_TYPE").toUpperCase();
            categories.add(mmoType);

            // Map common MMO types to generic categories
            if (mmoType.contains("SWORD") || mmoType.equals("KATANA") || mmoType.equals("RAPIER")
                    || mmoType.equals("DAGGER")) {
                categories.add("SWORD");
            } else if (mmoType.equals("HELMET") || mmoType.equals("CHESTPLATE") || mmoType.equals("LEGGINGS")
                    || mmoType.equals("BOOTS")) {
                categories.add("ARMOR");
            } else if (mmoType.equals("PICKAXE") || mmoType.equals("AXE") || mmoType.equals("SHOVEL")
                    || mmoType.equals("HOE")) {
                categories.add("TOOL");
            }
        }

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
        if (type.endsWith("_PICKAXE")) {
            categories.add("PICKAXE");
            categories.add("TOOL");
        }
        if (type.endsWith("_SHOVEL") || type.endsWith("_SPADE")) {
            categories.add("SHOVEL");
            categories.add("TOOL");
        }
        if (type.endsWith("_HOE")) {
            categories.add("HOE");
            categories.add("TOOL");
        }
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

    private List<EnchantConfig> getApplicableEnchants(ItemStack item) {
        List<EnchantConfig> applicableEnchants = new ArrayList<>();
        if (item == null || item.getType() == Material.AIR)
            return applicableEnchants;

        Set<String> itemCategories = getItemCategories(item);

        // 1. Add Vanilla Enchants
        for (EnchantConfig enchant : plugin.getEnchantManager().getEnchants().values()) {
            if (!enchant.isEnabled())
                continue;

            boolean matches = false;
            for (String target : enchant.getTargets()) {
                if (target.equalsIgnoreCase("GLOBAL") || itemCategories.contains(target.toUpperCase())) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                applicableEnchants.add(enchant);
            }
        }

        // 2. Add Custom Enchants
        for (CustomEnchant customEnchant : plugin.getCustomEnchantManager().getAllEnchants()) {
            if (!customEnchant.isEnabled())
                continue;

            EnchantConfig config = customEnchant.toEnchantConfig();

            boolean matches = false;
            for (String target : config.getTargets()) {
                if (target.equalsIgnoreCase("GLOBAL") || itemCategories.contains(target.toUpperCase())) {
                    matches = true;
                    break;
                }
            }
            if (matches) {
                applicableEnchants.add(config);
            }
        }

        // 3. Apply Sorting
        switch (sortOrder) {
            case ALPHABETICAL_AZ:
                applicableEnchants.sort((e1, e2) -> ChatColor.stripColor(ColorUtils.colorize(e1.getDisplayName()))
                        .compareTo(ChatColor.stripColor(ColorUtils.colorize(e2.getDisplayName()))));
                break;
            case ALPHABETICAL_ZA:
                applicableEnchants.sort((e1, e2) -> ChatColor.stripColor(ColorUtils.colorize(e2.getDisplayName()))
                        .compareTo(ChatColor.stripColor(ColorUtils.colorize(e1.getDisplayName()))));
                break;
            case LEVEL_HIGH_TO_LOW:
                applicableEnchants.sort((e1, e2) -> Integer.compare(e2.getRequiredEnchantingLevel(),
                        e1.getRequiredEnchantingLevel()));
                break;
            case LEVEL_LOW_TO_HIGH:
                applicableEnchants.sort((e1, e2) -> Integer.compare(e1.getRequiredEnchantingLevel(),
                        e2.getRequiredEnchantingLevel()));
                break;
        }

        return applicableEnchants;
    }

    public void setItemToEnchant(ItemStack item) {
        this.itemToEnchant = item;
    }

    private void applyEnchantmentImmediately(EnchantConfig enchant) {
        if (itemToEnchant == null)
            return;

        // Check if item already has this enchantment at this level (or higher)
        int currentLevel = 0;
        if (itemToEnchant.hasItemMeta() && itemToEnchant.getItemMeta().hasLore()) {
            Map<String, Integer> currentEnchants = plugin.getEnchantManager()
                    .parseLore(itemToEnchant.getItemMeta().getLore());
            currentLevel = currentEnchants.getOrDefault(enchant.getId().toLowerCase(), 0);
        }

        if (currentLevel >= 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("enchanting.upgrade-fail"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        LevelConfig levelConfig = enchant.getLevel(1);
        if (levelConfig == null)
            return;

        // Check XP levels
        int pwp = plugin.getEnchantManager().getPriorWorkPenalty(itemToEnchant);
        int totalCost = levelConfig.getXpCost() + pwp;

        // Check if too expensive
        int maxCost = plugin.getConfig().getInt("enchanting.max-cost", 50);
        boolean limitEnabled = plugin.getConfig().getBoolean("enchanting.limit-enabled", true);
        boolean bypassOp = plugin.getConfig().getBoolean("enchanting.bypass-limits-on-op", true);

        if (limitEnabled && totalCost > maxCost && !(player.isOp() && bypassOp)) {
            player.sendMessage(plugin.getConfigManager().getMessage("enchanting.too-expensive", "{max}",
                    String.valueOf(maxCost)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        if (player.getLevel() < totalCost) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessageRaw("errors.not-enough-xp")
                    .replace("{cost}", String.valueOf(totalCost))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return;
        }

        isProcessing = true;
        try {
            // Apply enchantment
            plugin.getEnchantManager().applyEnchantment(itemToEnchant, enchant.getId(), 1);
            player.setLevel(player.getLevel() - totalCost);

            // Update PWP
            plugin.getEnchantManager().incrementPriorWorkPenalty(itemToEnchant);
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);

            // AuraSkills XP Integration
            if (plugin.isAuraSkillsEnabled()) {
                double xpAmount = 5.0 + (1 * 2.0) + (enchant.getRequiredEnchantingLevel() / 10.0);
                plugin.getAuraSkillsHook().addXP(player, "enchanting", xpAmount);
            }

            String successMsg = plugin.getConfigManager().getMessageRaw("enchanting.success")
                    .replace("{enchant}", enchant.getDisplayName())
                    .replace("{level}", "")
                    .replace("{cost}", String.valueOf(totalCost));
            player.sendMessage(ColorUtils.colorize(successMsg));

            // Refresh the item in the inventory slot!
            inventory.setItem(ITEM_SLOT, itemToEnchant);
            updateEnchantments();
        } finally {
            isProcessing = false;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
