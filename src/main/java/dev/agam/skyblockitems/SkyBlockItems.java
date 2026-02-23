package dev.agam.skyblockitems;

import dev.agam.skyblockitems.abilities.AbilityManager;
import dev.agam.skyblockitems.enchantsystem.config.ConfigManager;
import dev.agam.skyblockitems.enchantsystem.managers.ChatInputManager;
import dev.agam.skyblockitems.enchantsystem.managers.CustomEnchantManager;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for SkyBlockItems.
 * Refactored for maximum stability:
 * - No external API imports in the main class (except Bukkit).
 * - All hooks are accessed via reflection-safe wrappers.
 * - Granular logging for every step.
 */
public class SkyBlockItems extends JavaPlugin {

    static {
        System.out.println("[SkyBlockItems] [JVM] Class loaded successfully.");
    }

    private static SkyBlockItems instance;
    private AbilityManager abilityManager;
    private dev.agam.skyblockitems.rarity.RarityManager rarityManager;
    private dev.agam.skyblockitems.reforge.ReforgeManager reforgeManager;
    private ConfigManager enchantConfigManager;
    private EnchantManager enchantManager;
    private CustomEnchantManager customEnchantManager;
    private ChatInputManager chatInputManager;
    private dev.agam.skyblockitems.enchantsystem.listeners.CustomEnchantListener customEnchantListener;

    private Object mmoItemsStatHook;
    private Object auraSkillsHook;
    private Object mmoEnchantHook;
    private dev.agam.skyblockitems.integration.VaultHook vaultHook;

    private boolean auraSkillsEnabled = false;
    private boolean mmoItemsEnabled = false;
    private boolean mythicLibEnabled = false;

    private org.bukkit.configuration.file.FileConfiguration abilitiesConfig;

    public static SkyBlockItems getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        System.out.println("[SkyBlockItems] [BOOT] onLoad triggered.");
        try {
            if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                System.out.println("[SkyBlockItems] [BOOT] WorldGuard detected, preparing flags...");
                dev.agam.skyblockitems.integration.WorldGuardHook.registerFlags();
            }
        } catch (Throwable e) {
            System.err.println("[SkyBlockItems] [ERROR] onLoad hook error: " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        System.out.println("[SkyBlockItems] [BOOT] onEnable starting...");

        try {
            // 1. Configs
            System.out.println("[SkyBlockItems] [BOOT] Step 1/7: Loading Configurations...");
            this.enchantConfigManager = new ConfigManager(this);
            loadCustomConfigs();
            System.out.println("[SkyBlockItems] [BOOT] Step 1/7 complete.");

            // 2. Core Managers
            System.out.println("[SkyBlockItems] [BOOT] Step 2/7: Initializing Managers...");
            this.chatInputManager = new ChatInputManager();
            this.enchantManager = new EnchantManager(this);
            this.customEnchantManager = new CustomEnchantManager(this);
            this.customEnchantListener = new dev.agam.skyblockitems.enchantsystem.listeners.CustomEnchantListener(this);
            try {
                this.abilityManager = new AbilityManager();
                this.abilityManager.registerAbilities();
            } catch (Throwable e) {
                System.err.println("[SkyBlockItems] [ERROR] Failed to initialize AbilityManager: " + e.getMessage());
            }

            try {
                this.reforgeManager = new dev.agam.skyblockitems.reforge.ReforgeManager(this);
                this.reforgeManager.loadConfig();
            } catch (Throwable e) {
                System.err.println("[SkyBlockItems] [ERROR] Failed to initialize ReforgeManager: " + e.getMessage());
            }
            System.out.println("[SkyBlockItems] [BOOT] Step 2/7 complete.");

            // 3. Plugin Integration
            System.out.println("[SkyBlockItems] [BOOT] Step 3/7: Hooking External APIs...");
            this.mythicLibEnabled = Bukkit.getPluginManager().isPluginEnabled("MythicLib");
            setupHooks();
            System.out.println("[SkyBlockItems] [BOOT] Step 3/7 complete.");

            // 4. Listeners
            System.out.println("[SkyBlockItems] [BOOT] Step 4/7: Registering Listeners...");
            registerAllListeners();
            System.out.println("[SkyBlockItems] [BOOT] Step 4/7 complete.");

            // 5. Rarity System
            System.out.println("[SkyBlockItems] [BOOT] Step 5/7: Starting Rarity System...");
            startRaritySystem();
            System.out.println("[SkyBlockItems] [BOOT] Step 5/7 complete.");

            // 6. Commands
            System.out.println("[SkyBlockItems] [BOOT] Step 6/7: Registering Commands...");
            registerAllCommands();
            System.out.println("[SkyBlockItems] [BOOT] Step 6/7 complete.");

            // 7. Background Tasks
            System.out.println("[SkyBlockItems] [BOOT] Step 7/7: Starting Passive Tasks...");
            new dev.agam.skyblockitems.tasks.PassiveAbilityTask().runTaskTimer(this, 20L, 20L);
            // Cooldown is now shown only in chat, not in item lore
            // new dev.agam.skyblockitems.tasks.CooldownLoreTask().runTaskTimer(this, 10L,
            // 10L);
            System.out.println("[SkyBlockItems] [BOOT] Step 7/7 complete.");

            getLogger().info("SkyBlockItems v" + getDescription().getVersion() + " enriched and enabled!");

        } catch (Throwable e) {
            System.err.println("[SkyBlockItems] [CRITICAL] Enable sequence failed!");
            e.printStackTrace();
        }
    }

    private void setupHooks() {
        // MMOItems Stats
        if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            try {
                dev.agam.skyblockitems.integration.MMOItemsHook hook = new dev.agam.skyblockitems.integration.MMOItemsHook();
                hook.registerStats();
                this.mmoItemsStatHook = hook;
                this.mmoItemsEnabled = true;

                // MMOItems Enchantment Hook
                this.mmoEnchantHook = new dev.agam.skyblockitems.enchantsystem.hooks.MMOItemsHook(this);
            } catch (Throwable e) {
                System.err.println("[SkyBlockItems] [ERROR] MMOItems integration failed: " + e.getMessage());
            }
        }

        // AuraSkills
        if (Bukkit.getPluginManager().isPluginEnabled("AuraSkills")) {
            try {
                this.auraSkillsHook = new dev.agam.skyblockitems.enchantsystem.hooks.AuraSkillsHook();
                this.auraSkillsEnabled = true;
            } catch (Throwable e) {
                System.err.println("[SkyBlockItems] [ERROR] AuraSkills integration failed: " + e.getMessage());
            }
        }

        // Vault Economy
        try {
            this.vaultHook = new dev.agam.skyblockitems.integration.VaultHook();
        } catch (Throwable e) {
            System.err.println("[SkyBlockItems] [ERROR] Vault integration failed: " + e.getMessage());
        }
    }

    private void registerAllListeners() {
        if (mythicLibEnabled) {
            try {
                getServer().getPluginManager().registerEvents(new dev.agam.skyblockitems.abilities.AbilityListener(),
                        this);
            } catch (Throwable e) {
                System.err.println("[SkyBlockItems] [ERROR] Failed to register AbilityListener: " + e.getMessage());
            }
        } else {
            System.err.println("[SkyBlockItems] [WARNING] MythicLib not found! Core abilities will be disabled.");
        }

        if (mmoItemsEnabled) {
            // AbilityLoreListener is deprecated and redundant (handled by
            // NaturalAbilityLoreStat)
        }

        if (mythicLibEnabled) {
            try {
                getServer().getPluginManager().registerEvents(
                        new dev.agam.skyblockitems.listeners.InfiniteReservoirListener(),
                        this);
            } catch (Throwable e) {
                System.err.println("[SkyBlockItems] [ERROR] Failed to register MythicLib listeners: " + e.getMessage());
            }
        }

        getServer().getPluginManager()
                .registerEvents(new dev.agam.skyblockitems.enchantsystem.listeners.GuiListener(this), this);
        getServer().getPluginManager().registerEvents(this.customEnchantListener, this);
        getServer().getPluginManager()
                .registerEvents(new dev.agam.skyblockitems.enchantsystem.listeners.BlockPlaceListener(this), this);

        if (auraSkillsEnabled) {
            try {
                getServer().getPluginManager().registerEvents(
                        new dev.agam.skyblockitems.enchantsystem.listeners.AuraSkillsListener(this), this);
            } catch (Throwable ignored) {
            }
        }

        // Reforge Listener
        getServer().getPluginManager()
                .registerEvents(new dev.agam.skyblockitems.reforge.ReforgeListener(this), this);
    }

    private void registerAllCommands() {
        dev.agam.skyblockitems.commands.SkyBlockItemsCommand sbi = new dev.agam.skyblockitems.commands.SkyBlockItemsCommand(
                this);
        getCommand("sbi").setExecutor(sbi);
        getCommand("sbi").setTabCompleter(sbi);
        getCommand("enchant").setExecutor(new dev.agam.skyblockitems.commands.EnchantCommand(this));
        getCommand("anvil").setExecutor(new dev.agam.skyblockitems.commands.AnvilCommand(this));
        getCommand("reforge").setExecutor(new dev.agam.skyblockitems.commands.ReforgeCommand(this));
        getCommand("blacksmith").setExecutor(new dev.agam.skyblockitems.commands.BlacksmithCommand(this));

        dev.agam.skyblockitems.rarity.RarityCommand rarityCmd = new dev.agam.skyblockitems.rarity.RarityCommand(this);
        getCommand("rarity").setExecutor(rarityCmd);
        getCommand("rarity").setTabCompleter(rarityCmd);
    }

    private void startRaritySystem() {
        try {
            this.rarityManager = new dev.agam.skyblockitems.rarity.RarityManager(this);
            getServer().getPluginManager().registerEvents(new dev.agam.skyblockitems.rarity.RarityListener(this), this);
            int interval = rarityManager.getCheckerTime();
            if (interval < 1)
                interval = 200;
            new dev.agam.skyblockitems.rarity.RarityTask(this).runTaskTimer(this, 100L, interval);
        } catch (Throwable e) {
            System.err.println("[SkyBlockItems] [ERROR] Rarity system start failure: " + e.getMessage());
        }
    }

    public void reloadAllConfigs() {
        // 1. Core Configs (messages, enchants, config.yml)
        enchantConfigManager.reload();

        // 2. Main plugin abilities.yml
        loadCustomConfigs();

        // 3. Rarity system
        if (rarityManager != null) {
            rarityManager.loadConfig();
        }

        // 4. Reforge system
        if (reforgeManager != null) {
            reforgeManager.reload();
        }

        getLogger().info("All configurations reloaded successfully!");
    }

    private void loadCustomConfigs() {
        // abilities.yml
        java.io.File file = new java.io.File(getDataFolder(), "abilities.yml");
        if (!file.exists())
            saveResource("abilities.yml", false);
        this.abilitiesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        System.out.println("[SkyBlockItems] [BOOT] Plugin disabled.");
    }

    // Accessors
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public dev.agam.skyblockitems.rarity.RarityManager getRarityManager() {
        return rarityManager;
    }

    public ConfigManager getConfigManager() {
        return enchantConfigManager;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public CustomEnchantManager getCustomEnchantManager() {
        return customEnchantManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public dev.agam.skyblockitems.enchantsystem.listeners.CustomEnchantListener getCustomEnchantListener() {
        return customEnchantListener;
    }

    // Safety Casting for Hooks
    public dev.agam.skyblockitems.enchantsystem.hooks.AuraSkillsHook getAuraSkillsHook() {
        return (dev.agam.skyblockitems.enchantsystem.hooks.AuraSkillsHook) auraSkillsHook;
    }

    public dev.agam.skyblockitems.enchantsystem.hooks.MMOItemsHook getMMOEnchantHook() {
        return (dev.agam.skyblockitems.enchantsystem.hooks.MMOItemsHook) mmoEnchantHook;
    }

    public boolean isAuraSkillsEnabled() {
        return auraSkillsEnabled;
    }

    public boolean isMMOItemsEnabled() {
        return mmoItemsEnabled;
    }

    public org.bukkit.configuration.file.FileConfiguration getAbilitiesConfig() {
        return abilitiesConfig;
    }

    public dev.agam.skyblockitems.reforge.ReforgeManager getReforgeManager() {
        return reforgeManager;
    }

    public dev.agam.skyblockitems.integration.VaultHook getVaultHook() {
        return vaultHook;
    }
}
